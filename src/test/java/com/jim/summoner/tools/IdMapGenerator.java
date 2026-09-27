package com.jim.summoner.tools;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class IdMapGenerator {

	private static final JsonMapper JSON_MAPPER =
			new JsonMapper();


	public static void main(String[] args)
			throws Exception {

		Path projectRoot =
				resolveProjectRoot();

		Path rawDirectory =
				projectRoot.resolve(
						Path.of(
								"data-source",
								"raw",
								"swarfarm"
						)
				);

		Path idMapDirectory =
				projectRoot.resolve(
						Path.of(
								"data-source",
								"id-map"
						)
				);


		Files.createDirectories(
				idMapDirectory
		);


		validateRawFiles(
				rawDirectory
		);


		System.out.println(
				"========================================"
		);

		System.out.println(
				"ID map generation start"
		);

		System.out.println(
				"raw    = "
				+ rawDirectory.toAbsolutePath()
		);

		System.out.println(
				"id-map = "
				+ idMapDirectory.toAbsolutePath()
		);

		System.out.println(
				"========================================"
		);


		// Monster
		generateCompositeIdMap(
				"monsters",
				rawDirectory.resolve(
						"monsters.json"
				),
				idMapDirectory.resolve(
						"monsters.json"
				),
				"id",
				"com2us_id"
		);


		// Skill
		generateCompositeIdMap(
				"skills",
				rawDirectory.resolve(
						"skills.json"
				),
				idMapDirectory.resolve(
						"skills.json"
				),
				"id",
				"com2us_id"
		);


		// Family
		// 별도 SWARFARM endpoint가 아니라
		// monsters.json의 family_id를 모아서 만든다.
		generateFamilyIdMap(
				rawDirectory.resolve(
						"monsters.json"
				),
				idMapDirectory.resolve(
						"families.json"
				)
		);


		// Leader Skill
		generateSimpleIdMap(
				"leader-skills",
				rawDirectory.resolve(
						"leader-skills.json"
				),
				idMapDirectory.resolve(
						"leader-skills.json"
				),
				"id"
		);


		// Skill Effect
		generateSimpleIdMap(
				"skill-effects",
				rawDirectory.resolve(
						"skill-effects.json"
				),
				idMapDirectory.resolve(
						"skill-effects.json"
				),
				"id"
		);


		// Monster Source
		generateSimpleIdMap(
				"monster-sources",
				rawDirectory.resolve(
						"monster-sources.json"
				),
				idMapDirectory.resolve(
						"monster-sources.json"
				),
				"id"
		);


		System.out.println();
		System.out.println(
				"========================================"
		);

		System.out.println(
				"ID map generation complete"
		);

		System.out.println(
				"========================================"
		);
	}


	// =========================================================
	// Monster / Skill
	//
	// SWARFARM ID + Com2uS ID를 모두 이용한다.
	// =========================================================

	private static void generateCompositeIdMap(
			String datasetName,
			Path rawFile,
			Path mapFile,
			String swarfarmIdField,
			String com2usIdField)
			throws Exception {

		System.out.println();
		System.out.println(
				"[ID-MAP] "
				+ datasetName
		);


		List<JsonNode> records =
				readJsonArray(
						rawFile
				);


		// 첫 생성 결과가 API 반환 순서에 영향을 받지 않도록
		// SWARFARM ID 기준으로 정렬한다.
		records.sort(
				Comparator.comparingLong(
						node ->
								readRequiredPositiveLong(
										node,
										swarfarmIdField,
										datasetName
								)
				)
		);


		IdMapState state =
				loadIdMap(
						mapFile
				);


		normalizeNextId(
				state
		);


		int allocatedBefore =
				state.nextId;


		Set<Long> seenSwarfarmIds =
				new HashSet<>();

		Map<Long, Long> seenCom2usIds =
				new HashMap<>();


		for (JsonNode record : records) {

			long swarfarmId =
					readRequiredPositiveLong(
							record,
							swarfarmIdField,
							datasetName
					);


			if (!seenSwarfarmIds.add(
					swarfarmId
			)) {

				throw new IllegalStateException(
						"raw 데이터에 중복 SWARFARM ID가 있습니다."
						+ " dataset="
						+ datasetName
						+ ", swarfarmId="
						+ swarfarmId
				);
			}


			Long com2usId =
					readOptionalPositiveLong(
							record,
							com2usIdField
					);


			if (com2usId != null) {

				Long previousSwarfarmId =
						seenCom2usIds.putIfAbsent(
								com2usId,
								swarfarmId
						);


				if (previousSwarfarmId != null
						&& previousSwarfarmId
								.longValue()
								!= swarfarmId) {

					throw new IllegalStateException(
							"서로 다른 현재 엔티티가 같은 Com2uS ID를 사용합니다."
							+ " dataset="
							+ datasetName
							+ ", com2usId="
							+ com2usId
							+ ", swarfarmId1="
							+ previousSwarfarmId
							+ ", swarfarmId2="
							+ swarfarmId
					);
				}
			}


			resolveCompositeInternalId(
					state,
					datasetName,
					swarfarmId,
					com2usId
			);
		}


		sortMappings(
				state
		);


		writeIdMap(
				mapFile,
				state
		);


		int newCount =
				state.nextId
				- allocatedBefore;


		System.out.println(
				"  raw entities       = "
				+ records.size()
		);

		System.out.println(
				"  total swarfarm map = "
				+ state.bySwarfarmId.size()
		);

		System.out.println(
				"  total com2us map   = "
				+ state.byCom2usId.size()
		);

		System.out.println(
				"  newly allocated    = "
				+ newCount
		);

		System.out.println(
				"  nextId             = "
				+ state.nextId
		);

		System.out.println(
				"  saved              = "
				+ mapFile.toAbsolutePath()
		);
	}


	private static int resolveCompositeInternalId(
			IdMapState state,
			String datasetName,
			long swarfarmId,
			Long com2usId) {

		String swarfarmKey =
				Long.toString(
						swarfarmId
				);


		Integer bySwarfarm =
				state.bySwarfarmId.get(
						swarfarmKey
				);


		Integer byCom2us =
				null;


		if (com2usId != null) {

			byCom2us =
					state.byCom2usId.get(
							Long.toString(
									com2usId
							)
					);
		}


		// 둘 다 이미 존재하는데 서로 다른 내부 ID를 가리키면
		// 자동 수정하지 않고 즉시 중단한다.
		if (bySwarfarm != null
				&& byCom2us != null
				&& !bySwarfarm.equals(
						byCom2us
				)) {

			throw new IllegalStateException(
					"ID map 충돌이 발생했습니다."
					+ " dataset="
					+ datasetName
					+ ", swarfarmId="
					+ swarfarmId
					+ " -> "
					+ bySwarfarm
					+ ", com2usId="
					+ com2usId
					+ " -> "
					+ byCom2us
			);
		}


		int internalId;


		if (bySwarfarm != null) {

			internalId =
					bySwarfarm;
		}
		else if (byCom2us != null) {

			// SWARFARM ID가 바뀌었지만
			// Com2uS ID가 기존과 같으면
			// 기존 internal ID를 그대로 사용한다.
			internalId =
					byCom2us;
		}
		else {

			internalId =
					allocateId(
							state
					);
		}


		state.bySwarfarmId.put(
				swarfarmKey,
				internalId
		);


		if (com2usId != null) {

			state.byCom2usId.put(
					Long.toString(
							com2usId
					),
					internalId
			);
		}


		return internalId;
	}


	// =========================================================
	// Leader Skill / Effect / Source
	//
	// SWARFARM ID 하나만 이용한다.
	// =========================================================

	private static void generateSimpleIdMap(
			String datasetName,
			Path rawFile,
			Path mapFile,
			String idField)
			throws Exception {

		System.out.println();
		System.out.println(
				"[ID-MAP] "
				+ datasetName
		);


		List<JsonNode> records =
				readJsonArray(
						rawFile
				);


		records.sort(
				Comparator.comparingLong(
						node ->
								readRequiredPositiveLong(
										node,
										idField,
										datasetName
								)
				)
		);


		IdMapState state =
				loadIdMap(
						mapFile
				);


		normalizeNextId(
				state
		);


		int allocatedBefore =
				state.nextId;


		Set<Long> seenIds =
				new HashSet<>();


		for (JsonNode record : records) {

			long sourceId =
					readRequiredPositiveLong(
							record,
							idField,
							datasetName
					);


			if (!seenIds.add(
					sourceId
			)) {

				throw new IllegalStateException(
						"raw 데이터에 중복 ID가 있습니다."
						+ " dataset="
						+ datasetName
						+ ", id="
						+ sourceId
				);
			}


			String key =
					Long.toString(
							sourceId
					);


			if (!state.bySwarfarmId.containsKey(
					key
			)) {

				state.bySwarfarmId.put(
						key,
						allocateId(
								state
						)
				);
			}
		}


		sortMappings(
				state
		);


		writeIdMap(
				mapFile,
				state
		);


		System.out.println(
				"  raw entities       = "
				+ records.size()
		);

		System.out.println(
				"  total map          = "
				+ state.bySwarfarmId.size()
		);

		System.out.println(
				"  newly allocated    = "
				+ (
						state.nextId
						- allocatedBefore
				)
		);

		System.out.println(
				"  nextId             = "
				+ state.nextId
		);

		System.out.println(
				"  saved              = "
				+ mapFile.toAbsolutePath()
		);
	}


	// =========================================================
	// Family
	//
	// monsters.json의 family_id를 이용한다.
	// =========================================================

	private static void generateFamilyIdMap(
			Path monstersRawFile,
			Path mapFile)
			throws Exception {

		System.out.println();
		System.out.println(
				"[ID-MAP] families"
		);


		List<JsonNode> monsters =
				readJsonArray(
						monstersRawFile
				);


		Set<Long> familyIds =
				new TreeSet<>();


		int missingFamilyCount =
				0;


		for (JsonNode monster : monsters) {

			Long familyId =
					readOptionalPositiveLong(
							monster,
							"family_id"
					);


			if (familyId == null) {

				missingFamilyCount++;
				continue;
			}


			familyIds.add(
					familyId
			);
		}


		IdMapState state =
				loadIdMap(
						mapFile
				);


		normalizeNextId(
				state
		);


		int allocatedBefore =
				state.nextId;


		for (Long familyId : familyIds) {

			String key =
					Long.toString(
							familyId
					);


			if (!state.bySwarfarmId.containsKey(
					key
			)) {

				state.bySwarfarmId.put(
						key,
						allocateId(
								state
						)
				);
			}
		}


		sortMappings(
				state
		);


		writeIdMap(
				mapFile,
				state
		);


		System.out.println(
				"  unique families    = "
				+ familyIds.size()
		);

		System.out.println(
				"  missing family_id  = "
				+ missingFamilyCount
		);

		System.out.println(
				"  total map          = "
				+ state.bySwarfarmId.size()
		);

		System.out.println(
				"  newly allocated    = "
				+ (
						state.nextId
						- allocatedBefore
				)
		);

		System.out.println(
				"  nextId             = "
				+ state.nextId
		);

		System.out.println(
				"  saved              = "
				+ mapFile.toAbsolutePath()
		);
	}


	// =========================================================
	// ID map 파일 읽기 / 저장
	// =========================================================

	private static IdMapState loadIdMap(
			Path mapFile)
			throws Exception {

		if (!Files.exists(
				mapFile
		)) {

			return new IdMapState();
		}


		try (InputStream inputStream =
				Files.newInputStream(
						mapFile
				)) {

			IdMapState state =
					JSON_MAPPER.readValue(
							inputStream,
							new TypeReference<
									IdMapState>() {}
					);


			if (state.bySwarfarmId == null) {

				state.bySwarfarmId =
						new LinkedHashMap<>();
			}


			if (state.byCom2usId == null) {

				state.byCom2usId =
						new LinkedHashMap<>();
			}


			return state;
		}
	}


	private static void writeIdMap(
			Path mapFile,
			IdMapState state)
			throws Exception {

		String json =
				JSON_MAPPER
						.writerWithDefaultPrettyPrinter()
						.writeValueAsString(
								state
						);


		Files.writeString(
				mapFile,
				json,
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING
		);
	}


	// =========================================================
	// ID 할당
	// =========================================================

	private static int allocateId(
			IdMapState state) {

		int id =
				state.nextId;


		state.nextId++;


		return id;
	}


	/*
	 * id-map 파일을 사람이 수정했거나
	 * 예전 버전에서 nextId가 잘못 저장되어 있어도
	 * 이미 사용된 internal ID를 재사용하지 않도록 보정한다.
	 */
	private static void normalizeNextId(
			IdMapState state) {

		int maxId =
				0;


		for (Integer value :
				state.bySwarfarmId.values()) {

			if (value != null) {

				maxId =
						Math.max(
								maxId,
								value
						);
			}
		}


		for (Integer value :
				state.byCom2usId.values()) {

			if (value != null) {

				maxId =
						Math.max(
								maxId,
								value
						);
			}
		}


		int minimumNextId =
				maxId + 1;


		if (state.nextId
				< minimumNextId) {

			state.nextId =
					minimumNextId;
		}


		if (state.nextId <= 0) {

			state.nextId =
					1;
		}
	}


	// =========================================================
	// Map 정렬
	//
	// JSON을 사람이 diff로 보기 쉽도록 외부 ID 기준 정렬한다.
	// =========================================================

	private static void sortMappings(
			IdMapState state) {

		state.bySwarfarmId =
				sortNumericKeyMap(
						state.bySwarfarmId
				);


		state.byCom2usId =
				sortNumericKeyMap(
						state.byCom2usId
				);
	}


	private static Map<String, Integer>
			sortNumericKeyMap(
					Map<String, Integer> source) {

		List<Map.Entry<String, Integer>> entries =
				new ArrayList<>(
						source.entrySet()
				);


		entries.sort(
				Comparator.comparingLong(
						entry ->
								Long.parseLong(
										entry.getKey()
								)
				)
		);


		Map<String, Integer> result =
				new LinkedHashMap<>();


		for (Map.Entry<String, Integer> entry :
				entries) {

			result.put(
					entry.getKey(),
					entry.getValue()
			);
		}


		return result;
	}


	// =========================================================
	// Raw JSON 읽기
	// =========================================================

	private static List<JsonNode> readJsonArray(
			Path file)
			throws Exception {

		try (InputStream inputStream =
				Files.newInputStream(
						file
				)) {

			JsonNode root =
					JSON_MAPPER.readTree(
							inputStream
					);


			if (root == null
					|| !root.isArray()) {

				throw new IllegalStateException(
						"JSON 루트가 배열이 아닙니다."
						+ " file="
						+ file.toAbsolutePath()
				);
			}


			List<JsonNode> result =
					new ArrayList<>();


			for (JsonNode node : root) {

				result.add(
						node
				);
			}


			return result;
		}
	}


	// =========================================================
	// JSON ID 값 읽기
	// =========================================================

	private static long readRequiredPositiveLong(
			JsonNode node,
			String fieldName,
			String datasetName) {

		Long value =
				readOptionalPositiveLong(
						node,
						fieldName
				);


		if (value == null) {

			throw new IllegalStateException(
					"필수 ID 값이 없거나 유효하지 않습니다."
					+ " dataset="
					+ datasetName
					+ ", field="
					+ fieldName
			);
		}


		return value;
	}


	private static Long readOptionalPositiveLong(
			JsonNode node,
			String fieldName) {

		JsonNode valueNode =
				node.get(
						fieldName
				);


		if (valueNode == null
				|| valueNode.isNull()) {

			return null;
		}


		long value;


		try {

			value =
					valueNode.asLong();
		}
		catch (Exception e) {

			return null;
		}


		if (value <= 0) {

			return null;
		}


		return value;
	}


	// =========================================================
	// Raw 파일 존재 여부
	// =========================================================

	private static void validateRawFiles(
			Path rawDirectory) {

		List<String> requiredFiles =
				List.of(
						"monsters.json",
						"skills.json",
						"leader-skills.json",
						"skill-effects.json",
						"monster-sources.json"
				);


		for (String fileName :
				requiredFiles) {

			Path file =
					rawDirectory.resolve(
							fileName
					);


			if (!Files.exists(
					file
			)) {

				throw new IllegalStateException(
						"필수 raw 파일이 없습니다."
						+ " file="
						+ file.toAbsolutePath()
				);
			}
		}
	}


	// =========================================================
	// 프로젝트 루트 찾기
	// =========================================================

	private static Path resolveProjectRoot() {

		Path current =
				Path.of(
						System.getProperty(
								"user.dir"
						)
				)
				.toAbsolutePath()
				.normalize();


		Path cursor =
				current;


		while (cursor != null) {

			if (Files.exists(
					cursor.resolve(
							"pom.xml"
					)
			)) {

				return cursor;
			}


			cursor =
					cursor.getParent();
		}


		throw new IllegalStateException(
				"프로젝트 루트를 찾을 수 없습니다."
				+ " pom.xml이 있는 프로젝트에서 실행해주세요."
				+ " user.dir="
				+ current
		);
	}


	// =========================================================
	// ID map 파일 구조
	// =========================================================

	public static class IdMapState {

		public int nextId = 1;

		public Map<String, Integer> bySwarfarmId =
				new LinkedHashMap<>();

		public Map<String, Integer> byCom2usId =
				new LinkedHashMap<>();
	}
}