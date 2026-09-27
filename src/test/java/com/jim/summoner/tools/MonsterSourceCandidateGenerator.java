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
import java.util.Map;
import java.util.TreeMap;

import com.jim.summoner.data.model.MonsterSourceData;
import com.jim.summoner.data.model.MonsterSourceLocalization;
import com.jim.summoner.data.model.SourceIds;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class MonsterSourceCandidateGenerator {

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
								"monster-sources.json"
						)
				);


		Path idMapFile =
				projectRoot.resolve(
						Path.of(
								"data-source",
								"id-map",
								"monster-sources.json"
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


		Path candidateLocalizationEnDirectory =
				projectRoot.resolve(
						Path.of(
								"data-source",
								"candidate",
								"localization",
								"en"
						)
				);


		Files.createDirectories(
				candidateNormalizedDirectory
		);


		Files.createDirectories(
				candidateLocalizationEnDirectory
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
				"Monster source candidate generation start"
		);

		System.out.println(
				"========================================"
		);


		List<JsonNode> rawSources =
				readJsonArray(
						rawFile
				);


		Map<Long, Integer> sourceIdMap =
				loadSwarfarmIdMap(
						idMapFile
				);


		GenerationResult result =
				generate(
						rawSources,
						sourceIdMap
				);


		writeJson(
				candidateNormalizedDirectory.resolve(
						"monster-sources.json"
				),
				result.sources()
		);


		writeJson(
				candidateLocalizationEnDirectory.resolve(
						"monster-sources.json"
				),
				result.localizations()
		);


		System.out.println();

		System.out.println(
				"monster sources  = "
				+ result.sources()
						.size()
		);

		System.out.println(
				"localizations    = "
				+ result.localizations()
						.size()
		);


		System.out.println();

		System.out.println(
				"[FARMABLE]"
		);

		System.out.println(
				"true  = "
				+ result.stats()
						.farmableTrueCount
		);

		System.out.println(
				"false = "
				+ result.stats()
						.farmableFalseCount
		);


		System.out.println();

		System.out.println(
				"[DESCRIPTION]"
		);

		System.out.println(
				"with description    = "
				+ result.stats()
						.withDescriptionCount
		);

		System.out.println(
				"without description = "
				+ result.stats()
						.withoutDescriptionCount
		);


		System.out.println();

		System.out.println(
				"========================================"
		);

		System.out.println(
				"Monster source candidate generation complete"
		);

		System.out.println(
				"========================================"
		);
	}


	// =========================================================
	// 전체 생성
	// =========================================================

	private static GenerationResult generate(
			List<JsonNode> rawSources,
			Map<Long, Integer> sourceIdMap) {

		List<MonsterSourceData> sources =
				new ArrayList<>();


		Map<Integer, MonsterSourceLocalization> localizations =
				new TreeMap<>();


		GenerationStats stats =
				new GenerationStats();


		for (JsonNode raw :
				rawSources) {

			MonsterSourceData source =
					convertSource(
							raw,
							sourceIdMap,
							stats
					);


			MonsterSourceLocalization localization =
					convertLocalization(
							raw,
							stats
					);


			sources.add(
					source
			);


			MonsterSourceLocalization previous =
					localizations.put(
							source.getId(),
							localization
					);


			if (previous != null) {

				throw new IllegalStateException(
						"중복 내부 MonsterSource ID가 있습니다."
						+ " internalId="
						+ source.getId()
				);
			}
		}


		sources.sort(
				Comparator.comparingInt(
						MonsterSourceData::getId
				)
		);


		return new GenerationResult(
				sources,
				localizations,
				stats
		);
	}


	// =========================================================
	// MonsterSource 변환
	// =========================================================

	private static MonsterSourceData convertSource(
			JsonNode raw,
			Map<Long, Integer> sourceIdMap,
			GenerationStats stats) {

		long swarfarmId =
				requiredPositiveLong(
						raw,
						"id",
						"monster source"
				);


		int internalId =
				requireMappedId(
						sourceIdMap,
						swarfarmId,
						"monster source"
				);


		boolean farmable =
				requiredBoolean(
						raw,
						"farmable_source",
						"monster source "
						+ swarfarmId
				);


		MonsterSourceData source =
				new MonsterSourceData();


		source.setId(
				internalId
		);


		SourceIds sourceIds =
				new SourceIds();


		sourceIds.setSwarfarm(
				Math.toIntExact(
						swarfarmId
				)
		);


		source.setSourceIds(
				sourceIds
		);


		source.setFarmable(
				farmable
		);


		if (farmable) {

			stats.farmableTrueCount++;
		}
		else {

			stats.farmableFalseCount++;
		}


		return source;
	}


	// =========================================================
	// Localization
	// =========================================================

	private static MonsterSourceLocalization convertLocalization(
			JsonNode raw,
			GenerationStats stats) {

		long swarfarmId =
				requiredPositiveLong(
						raw,
						"id",
						"monster source localization"
				);


		String name =
				requiredString(
						raw,
						"name",
						"monster source "
						+ swarfarmId
				);


		/*
		 * 실제 raw에는 빈 문자열 description이 많다.
		 *
		 * 의미 없는 null 변환을 하지 않고
		 * 원본 문자열을 그대로 보존한다.
		 */
		String description =
				requiredPresentString(
						raw,
						"description",
						"monster source "
						+ swarfarmId
				);


		MonsterSourceLocalization localization =
				new MonsterSourceLocalization();


		localization.setName(
				name
		);


		localization.setDescription(
				description
		);


		if (description.isBlank()) {

			stats.withoutDescriptionCount++;
		}
		else {

			stats.withDescriptionCount++;
		}


		return localization;
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


	private static boolean requiredBoolean(
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
					"필수 boolean 값이 없습니다."
					+ " context="
					+ context
					+ ", field="
					+ field
			);
		}


		return valueNode.asBoolean();
	}


	private static String requiredString(
			JsonNode node,
			String field,
			String context) {

		String value =
				requiredPresentString(
						node,
						field,
						context
				);


		if (value.isBlank()) {

			throw new IllegalStateException(
					"필수 문자열 값이 비어 있습니다."
					+ " context="
					+ context
					+ ", field="
					+ field
			);
		}


		return value;
	}


	private static String requiredPresentString(
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
					"필수 문자열 값이 없습니다."
					+ " context="
					+ context
					+ ", field="
					+ field
			);
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

		private int farmableTrueCount;

		private int farmableFalseCount;

		private int withDescriptionCount;

		private int withoutDescriptionCount;
	}


	private record GenerationResult(

			List<MonsterSourceData> sources,

			Map<Integer, MonsterSourceLocalization> localizations,

			GenerationStats stats) {
	}
}