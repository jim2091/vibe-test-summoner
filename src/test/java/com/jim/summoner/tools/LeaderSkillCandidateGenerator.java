package com.jim.summoner.tools;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.jim.summoner.data.model.LeaderSkillData;
import com.jim.summoner.data.model.SourceIds;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class LeaderSkillCandidateGenerator {

	private static final JsonMapper JSON_MAPPER =
			new JsonMapper();


	public static void main(String[] args)
			throws Exception {

		Path projectRoot =
				resolveProjectRoot();


		Path rawFile =
				projectRoot.resolve(
						Path.of(
								"data-source",
								"raw",
								"swarfarm",
								"leader-skills.json"
						)
				);


		Path idMapFile =
				projectRoot.resolve(
						Path.of(
								"data-source",
								"id-map",
								"leader-skills.json"
						)
				);


		Path candidateNormalizedDirectory =
				projectRoot.resolve(
						Path.of(
								"data-source",
								"candidate",
								"normalized"
						)
				);


		Files.createDirectories(
				candidateNormalizedDirectory
		);


		validateRequiredFile(
				rawFile
		);


		validateRequiredFile(
				idMapFile
		);


		System.out.println(
				"========================================"
		);

		System.out.println(
				"Leader skill candidate generation start"
		);

		System.out.println(
				"========================================"
		);


		List<JsonNode> rawLeaderSkills =
				readJsonArray(
						rawFile
				);


		Map<Long, Integer> leaderSkillIdMap =
				loadSwarfarmIdMap(
						idMapFile
				);


		GenerationResult result =
				generate(
						rawLeaderSkills,
						leaderSkillIdMap
				);


		writeJson(
				candidateNormalizedDirectory.resolve(
						"leader-skills.json"
				),
				result.leaderSkills()
		);


		System.out.println();

		System.out.println(
				"leader skills = "
				+ result.leaderSkills()
						.size()
		);


		System.out.println();

		System.out.println(
				"[STAT]"
		);


		for (Map.Entry<String, Integer> entry :
				result.stats()
						.statCounts
						.entrySet()) {

			System.out.println(
					entry.getKey()
					+ " = "
					+ entry.getValue()
			);
		}


		System.out.println();

		System.out.println(
				"[AREA]"
		);


		for (Map.Entry<String, Integer> entry :
				result.stats()
						.areaCounts
						.entrySet()) {

			System.out.println(
					entry.getKey()
					+ " = "
					+ entry.getValue()
			);
		}


		System.out.println();

		System.out.println(
				"[ELEMENT]"
		);

		System.out.println(
				"NULL = "
				+ result.stats()
						.nullElementCount
		);


		for (Map.Entry<String, Integer> entry :
				result.stats()
						.elementCounts
						.entrySet()) {

			System.out.println(
					entry.getKey()
					+ " = "
					+ entry.getValue()
			);
		}


		System.out.println();

		System.out.println(
				"========================================"
		);

		System.out.println(
				"Leader skill candidate generation complete"
		);

		System.out.println(
				"========================================"
		);
	}


	// =========================================================
	// 전체 생성
	// =========================================================

	private static GenerationResult generate(
			List<JsonNode> rawLeaderSkills,
			Map<Long, Integer> leaderSkillIdMap) {

		List<LeaderSkillData> result =
				new ArrayList<>();


		GenerationStats stats =
				new GenerationStats();


		for (JsonNode raw :
				rawLeaderSkills) {

			LeaderSkillData leaderSkill =
					convertLeaderSkill(
							raw,
							leaderSkillIdMap,
							stats
					);


			result.add(
					leaderSkill
			);
		}


		result.sort(
				Comparator.comparingInt(
						LeaderSkillData::getId
				)
		);


		return new GenerationResult(
				result,
				stats
		);
	}


	// =========================================================
	// LeaderSkill 변환
	// =========================================================

	private static LeaderSkillData convertLeaderSkill(
			JsonNode raw,
			Map<Long, Integer> leaderSkillIdMap,
			GenerationStats stats) {

		long swarfarmId =
				requiredPositiveLong(
						raw,
						"id",
						"leader skill"
				);


		int internalId =
				requireMappedId(
						leaderSkillIdMap,
						swarfarmId,
						"leader skill"
				);


		String stat =
				convertStat(
						requiredString(
								raw,
								"attribute",
								"leader skill "
								+ swarfarmId
						)
				);


		int amount =
				requiredInt(
						raw,
						"amount",
						"leader skill "
						+ swarfarmId
				);


		String area =
				convertArea(
						requiredString(
								raw,
								"area",
								"leader skill "
								+ swarfarmId
						)
				);


		String element =
				convertElement(
						optionalString(
								raw,
								"element"
						)
				);


		LeaderSkillData leaderSkill =
				new LeaderSkillData();


		leaderSkill.setId(
				internalId
		);


		SourceIds sourceIds =
				new SourceIds();


		sourceIds.setSwarfarm(
				Math.toIntExact(
						swarfarmId
				)
		);


		leaderSkill.setSourceIds(
				sourceIds
		);


		leaderSkill.setStat(
				stat
		);


		leaderSkill.setAmount(
				amount
		);


		leaderSkill.setArea(
				area
		);


		leaderSkill.setElement(
				element
		);


		stats.statCounts.put(
				stat,
				stats.statCounts.getOrDefault(
						stat,
						0
				) + 1
		);


		stats.areaCounts.put(
				area,
				stats.areaCounts.getOrDefault(
						area,
						0
				) + 1
		);


		if (element == null) {

			stats.nullElementCount++;
		}
		else {

			stats.elementCounts.put(
					element,
					stats.elementCounts.getOrDefault(
							element,
							0
					) + 1
			);
		}


		return leaderSkill;
	}


	// =========================================================
	// Stat
	// =========================================================

	private static String convertStat(
			String value) {

		return switch (
				value.trim()
						.toLowerCase(
								Locale.ROOT
						)
		) {

		case "accuracy" ->
			"ACCURACY";

		case "attack power" ->
			"ATTACK";

		case "attack speed" ->
			"SPEED";

		case "critical rate" ->
			"CRIT_RATE";

		case "critical dmg" ->
			"CRIT_DAMAGE";

		case "defense" ->
			"DEFENSE";

		case "hp" ->
			"HP";

		case "resistance" ->
			"RESISTANCE";

		default ->
			throw new IllegalStateException(
					"알 수 없는 LeaderSkill attribute입니다."
					+ " value="
					+ value
			);
		};
	}


	// =========================================================
	// Area
	// =========================================================

	private static String convertArea(
			String value) {

		return switch (
				value.trim()
						.toLowerCase(
								Locale.ROOT
						)
		) {

		case "general" ->
			"GENERAL";

		case "arena" ->
			"ARENA";

		case "guild" ->
			"GUILD";

		case "dungeon" ->
			"DUNGEON";

		case "element" ->
			"ELEMENT";

		default ->
			throw new IllegalStateException(
					"알 수 없는 LeaderSkill area입니다."
					+ " value="
					+ value
			);
		};
	}


	// =========================================================
	// Element
	// =========================================================

	private static String convertElement(
			String value) {

		if (value == null
				|| value.isBlank()) {

			return null;
		}


		return switch (
				value.trim()
						.toLowerCase(
								Locale.ROOT
						)
		) {

		case "fire" ->
			"FIRE";

		case "water" ->
			"WATER";

		case "wind" ->
			"WIND";

		case "light" ->
			"LIGHT";

		case "dark" ->
			"DARK";

		default ->
			throw new IllegalStateException(
					"알 수 없는 LeaderSkill element입니다."
					+ " value="
					+ value
			);
		};
	}


	// =========================================================
	// ID Map
	// =========================================================

	private static Map<Long, Integer> loadSwarfarmIdMap(
			Path file)
			throws Exception {

		IdMapState state;


		try (InputStream inputStream =
				Files.newInputStream(
						file
				)) {

			state =
					JSON_MAPPER.readValue(
							inputStream,
							new TypeReference<
									IdMapState>() {}
					);
		}


		if (state.bySwarfarmId
				== null) {

			throw new IllegalStateException(
					"ID map에 bySwarfarmId가 없습니다."
					+ " file="
					+ file.toAbsolutePath()
			);
		}


		Map<Long, Integer> result =
				new LinkedHashMap<>();


		for (Map.Entry<String, Integer> entry :
				state.bySwarfarmId.entrySet()) {

			long sourceId;


			try {

				sourceId =
						Long.parseLong(
								entry.getKey()
						);
			}
			catch (NumberFormatException e) {

				throw new IllegalStateException(
						"ID map의 SWARFARM ID가 숫자가 아닙니다."
						+ " file="
						+ file.toAbsolutePath()
						+ ", id="
						+ entry.getKey(),
						e
				);
			}


			Integer previous =
					result.put(
							sourceId,
							entry.getValue()
					);


			if (previous != null) {

				throw new IllegalStateException(
						"중복 SWARFARM ID가 있습니다."
						+ " file="
						+ file.toAbsolutePath()
						+ ", id="
						+ sourceId
				);
			}
		}


		return result;
	}


	private static int requireMappedId(
			Map<Long, Integer> map,
			long sourceId,
			String type) {

		Integer internalId =
				map.get(
						sourceId
				);


		if (internalId == null) {

			throw new IllegalStateException(
					"ID map에서 값을 찾을 수 없습니다."
					+ " type="
					+ type
					+ ", sourceId="
					+ sourceId
			);
		}


		return internalId;
	}


	// =========================================================
	// Raw 값 읽기
	// =========================================================

	private static long requiredPositiveLong(
			JsonNode node,
			String field,
			String context) {

		JsonNode valueNode =
				node.get(
						field
				);


		if (valueNode == null
				|| valueNode.isNull()) {

			throw new IllegalStateException(
					"필수 양수 값이 없습니다."
					+ " context="
					+ context
					+ ", field="
					+ field
			);
		}


		long value =
				valueNode.asLong();


		if (value <= 0) {

			throw new IllegalStateException(
					"필수 양수 값이 올바르지 않습니다."
					+ " context="
					+ context
					+ ", field="
					+ field
					+ ", value="
					+ value
			);
		}


		return value;
	}


	private static int requiredInt(
			JsonNode node,
			String field,
			String context) {

		JsonNode valueNode =
				node.get(
						field
				);


		if (valueNode == null
				|| valueNode.isNull()) {

			throw new IllegalStateException(
					"필수 정수 값이 없습니다."
					+ " context="
					+ context
					+ ", field="
					+ field
			);
		}


		return valueNode.asInt();
	}


	private static String requiredString(
			JsonNode node,
			String field,
			String context) {

		String value =
				optionalString(
						node,
						field
				);


		if (value == null
				|| value.isBlank()) {

			throw new IllegalStateException(
					"필수 문자열 값이 없습니다."
					+ " context="
					+ context
					+ ", field="
					+ field
			);
		}


		return value;
	}


	private static String optionalString(
			JsonNode node,
			String field) {

		JsonNode valueNode =
				node.get(
						field
				);


		if (valueNode == null
				|| valueNode.isNull()) {

			return null;
		}


		return valueNode.asString();
	}


	// =========================================================
	// Raw JSON
	// =========================================================

	private static List<JsonNode> readJsonArray(
			Path file)
			throws Exception {

		try (InputStream inputStream =
				Files.newInputStream(
						file
				)) {

			return JSON_MAPPER.readValue(
					inputStream,
					new TypeReference<
							List<JsonNode>>() {}
			);
		}
	}


	// =========================================================
	// JSON 저장
	// =========================================================

	private static void writeJson(
			Path file,
			Object value)
			throws Exception {

		String json =
				JSON_MAPPER
						.writerWithDefaultPrettyPrinter()
						.writeValueAsString(
								value
						);


		Files.writeString(
				file,
				json,
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING
		);


		System.out.println(
				"[SAVED] "
				+ file.toAbsolutePath()
		);
	}


	// =========================================================
	// 필수 파일 확인
	// =========================================================

	private static void validateRequiredFile(
			Path file) {

		if (!Files.exists(
				file
		)) {

			throw new IllegalStateException(
					"필수 파일이 없습니다. file="
					+ file.toAbsolutePath()
			);
		}
	}


	// =========================================================
	// 프로젝트 루트
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
				+ " user.dir="
				+ current
		);
	}


	// =========================================================
	// 내부 보조 클래스
	// =========================================================

	public static class IdMapState {

		public int nextId;

		public Map<String, Integer> bySwarfarmId =
				new LinkedHashMap<>();

		public Map<String, Integer> byCom2usId =
				new LinkedHashMap<>();
	}


	private static class GenerationStats {

		private final Map<String, Integer> statCounts =
				new TreeMap<>();

		private final Map<String, Integer> areaCounts =
				new TreeMap<>();

		private final Map<String, Integer> elementCounts =
				new TreeMap<>();

		private int nullElementCount;
	}


	private record GenerationResult(

			List<LeaderSkillData> leaderSkills,

			GenerationStats stats) {
	}
}