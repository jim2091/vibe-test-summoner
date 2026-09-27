package com.jim.summoner.tools;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.jim.summoner.data.model.AssetData;
import com.jim.summoner.data.model.SkillEffectData;
import com.jim.summoner.data.model.SkillEffectFlags;
import com.jim.summoner.data.model.SkillEffectLocalization;
import com.jim.summoner.data.model.SourceIds;
import com.jim.summoner.data.type.SkillEffectType;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class SkillEffectCandidateGenerator {

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
								"skill-effects.json"
						)
				);


		Path idMapFile =
				projectRoot.resolve(
						Path.of(
								"data-source",
								"id-map",
								"skill-effects.json"
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
				"Skill effect candidate generation start"
		);

		System.out.println(
				"========================================"
		);


		List<JsonNode> rawEffects =
				readJsonArray(
						rawFile
				);


		Map<Long, Integer> effectIdMap =
				loadSwarfarmIdMap(
						idMapFile
				);


		GenerationResult result =
				generate(
						rawEffects,
						effectIdMap
				);


		writeJson(
				candidateNormalizedDirectory.resolve(
						"skill-effects.json"
				),
				result.effects()
		);


		writeJson(
				candidateLocalizationEnDirectory.resolve(
						"skill-effects.json"
				),
				result.localizations()
		);


		System.out.println();

		System.out.println(
				"skill effects  = "
				+ result.effects()
						.size()
		);

		System.out.println(
				"localizations  = "
				+ result.localizations()
						.size()
		);


		System.out.println();

		System.out.println(
				"[TYPE]"
		);


		for (SkillEffectType type :
				SkillEffectType.values()) {

			System.out.println(
					type
					+ " = "
					+ result.stats()
							.typeCounts
							.getOrDefault(
									type,
									0
							)
			);
		}


		System.out.println();

		System.out.println(
				"[BUFF FLAG]"
		);

		System.out.println(
				"true  = "
				+ result.stats()
						.buffTrueCount
		);

		System.out.println(
				"false = "
				+ result.stats()
						.buffFalseCount
		);


		System.out.println();

		System.out.println(
				"[ASSET]"
		);

		System.out.println(
				"with icon    = "
				+ result.stats()
						.withIconCount
		);

		System.out.println(
				"without icon = "
				+ result.stats()
						.withoutIconCount
		);


		System.out.println();

		System.out.println(
				"========================================"
		);

		System.out.println(
				"Skill effect candidate generation complete"
		);

		System.out.println(
				"========================================"
		);
	}


	// =========================================================
	// 전체 생성
	// =========================================================

	private static GenerationResult generate(
			List<JsonNode> rawEffects,
			Map<Long, Integer> effectIdMap) {

		List<SkillEffectData> effects =
				new ArrayList<>();


		Map<Integer, SkillEffectLocalization> localizations =
				new TreeMap<>();


		GenerationStats stats =
				new GenerationStats();


		for (JsonNode rawEffect :
				rawEffects) {

			SkillEffectData effect =
					convertEffect(
							rawEffect,
							effectIdMap,
							stats
					);


			SkillEffectLocalization localization =
					convertLocalization(
							rawEffect
					);


			effects.add(
					effect
			);


			SkillEffectLocalization previous =
					localizations.put(
							effect.getId(),
							localization
					);


			if (previous != null) {

				throw new IllegalStateException(
						"중복 내부 SkillEffect ID가 있습니다."
						+ " internalId="
						+ effect.getId()
				);
			}
		}


		effects.sort(
				Comparator.comparingInt(
						SkillEffectData::getId
				)
		);


		return new GenerationResult(
				effects,
				localizations,
				stats
		);
	}


	// =========================================================
	// SkillEffect 변환
	// =========================================================

	private static SkillEffectData convertEffect(
			JsonNode raw,
			Map<Long, Integer> effectIdMap,
			GenerationStats stats) {

		long swarfarmId =
				requiredPositiveLong(
						raw,
						"id",
						"skill effect"
				);


		int internalId =
				requireMappedId(
						effectIdMap,
						swarfarmId,
						"skill effect"
				);


		SkillEffectType type =
				convertType(
						requiredString(
								raw,
								"type",
								"skill effect "
								+ swarfarmId
						)
				);


		boolean buff =
				requiredBoolean(
						raw,
						"is_buff",
						"skill effect "
						+ swarfarmId
				);


		String iconFilename =
				requiredPresentString(
						raw,
						"icon_filename",
						"skill effect "
						+ swarfarmId
				);


		SkillEffectData effect =
				new SkillEffectData();


		effect.setId(
				internalId
		);


		SourceIds sourceIds =
				new SourceIds();


		sourceIds.setSwarfarm(
				Math.toIntExact(
						swarfarmId
				)
		);


		effect.setSourceIds(
				sourceIds
		);


		effect.setType(
				type
		);


		SkillEffectFlags flags =
				new SkillEffectFlags();


		flags.setBuff(
				buff
		);


		effect.setFlags(
				flags
		);


		/*
		 * raw icon_filename이 빈 문자열이면
		 * 실제 아이콘이 없는 것으로 보존한다.
		 *
		 * 외부 파일명 자체는 canonical에 저장하지 않고,
		 * 이미지가 존재하는 경우에만 내부 asset key를 만든다.
		 */
		if (iconFilename.isBlank()) {

			effect.setAssets(
					null
			);


			stats.withoutIconCount++;
		}
		else {

			AssetData assets =
					new AssetData();


			assets.setIconKey(
					"effect-"
					+ internalId
			);


			effect.setAssets(
					assets
			);


			stats.withIconCount++;
		}


		stats.typeCounts.put(
				type,
				stats.typeCounts.getOrDefault(
						type,
						0
				) + 1
		);


		if (buff) {

			stats.buffTrueCount++;
		}
		else {

			stats.buffFalseCount++;
		}


		return effect;
	}


	// =========================================================
	// Localization
	// =========================================================

	private static SkillEffectLocalization convertLocalization(
			JsonNode raw) {

		long swarfarmId =
				requiredPositiveLong(
						raw,
						"id",
						"skill effect localization"
				);


		SkillEffectLocalization localization =
				new SkillEffectLocalization();


		localization.setName(
				requiredString(
						raw,
						"name",
						"skill effect "
						+ swarfarmId
				)
		);


		localization.setDescription(
				requiredString(
						raw,
						"description",
						"skill effect "
						+ swarfarmId
				)
		);


		return localization;
	}


	// =========================================================
	// Type
	// =========================================================

	private static SkillEffectType convertType(
			String value) {

		return switch (
				value.trim()
						.toLowerCase(
								Locale.ROOT
						)
		) {

		case "buff" ->
			SkillEffectType.BUFF;

		case "debuff" ->
			SkillEffectType.DEBUFF;

		case "neutral" ->
			SkillEffectType.NEUTRAL;

		default ->
			throw new IllegalStateException(
					"알 수 없는 SkillEffect type입니다."
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

		private final Map<SkillEffectType, Integer> typeCounts =
				new EnumMap<>(
						SkillEffectType.class
				);

		private int buffTrueCount;

		private int buffFalseCount;

		private int withIconCount;

		private int withoutIconCount;
	}


	private record GenerationResult(

			List<SkillEffectData> effects,

			Map<Integer, SkillEffectLocalization> localizations,

			GenerationStats stats) {
	}
}