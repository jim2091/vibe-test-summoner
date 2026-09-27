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

import com.jim.summoner.data.model.AssetData;
import com.jim.summoner.data.model.SkillData;
import com.jim.summoner.data.model.SkillEffectRefData;
import com.jim.summoner.data.model.SkillEffectRefFlags;
import com.jim.summoner.data.model.SkillFlags;
import com.jim.summoner.data.model.SkillLevelUpData;
import com.jim.summoner.data.model.SkillLocalization;
import com.jim.summoner.data.model.SkillScaling;
import com.jim.summoner.data.model.SourceIds;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class SkillCandidateGenerator {

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


		Path skillsFile =
				rawDirectory.resolve(
						"skills.json"
				);


		Path skillIdMapFile =
				idMapDirectory.resolve(
						"skills.json"
				);


		Path skillEffectIdMapFile =
				idMapDirectory.resolve(
						"skill-effects.json"
				);


		validateRequiredFile(
				skillsFile
		);


		validateRequiredFile(
				skillIdMapFile
		);


		validateRequiredFile(
				skillEffectIdMapFile
		);


		System.out.println(
				"========================================"
		);

		System.out.println(
				"Skill candidate generation start"
		);

		System.out.println(
				"========================================"
		);


		List<JsonNode> rawSkills =
				readJsonArray(
						skillsFile
				);


		Map<Long, Integer> skillIdMap =
				loadSwarfarmIdMap(
						skillIdMapFile
				);


		Map<Long, Integer> skillEffectIdMap =
				loadSwarfarmIdMap(
						skillEffectIdMapFile
				);


		GenerationResult result =
				generate(
						rawSkills,
						skillIdMap,
						skillEffectIdMap
				);


		writeJson(
				candidateNormalizedDirectory.resolve(
						"skills.json"
				),
				result.skills()
		);


		writeJson(
				candidateLocalizationEnDirectory.resolve(
						"skills.json"
				),
				result.localizations()
		);


		System.out.println();

		System.out.println(
				"skills                  = "
				+ result.skills().size()
		);

		System.out.println(
				"localizations           = "
				+ result.localizations().size()
		);

		System.out.println(
				"effect references        = "
				+ result.stats().effectReferenceCount
		);

		System.out.println(
				"level ups                = "
				+ result.stats().levelUpCount
		);

		System.out.println(
				"related skill references = "
				+ result.stats().relatedSkillReferenceCount
		);

		System.out.println(
				"null cooldowns           = "
				+ result.stats().nullCooldownCount
		);

		System.out.println(
				"maxLevel mismatches      = "
				+ result.stats().maxLevelMismatchCount
		);


		System.out.println();

		System.out.println(
				"========================================"
		);

		System.out.println(
				"Skill candidate generation complete"
		);

		System.out.println(
				"========================================"
		);
	}


	// =========================================================
	// 전체 생성
	// =========================================================

	private static GenerationResult generate(
			List<JsonNode> rawSkills,
			Map<Long, Integer> skillIdMap,
			Map<Long, Integer> skillEffectIdMap)
			throws Exception {

		List<SkillData> skills =
				new ArrayList<>();


		Map<Integer, SkillLocalization> localizations =
				new TreeMap<>();


		GenerationStats stats =
				new GenerationStats();


		for (JsonNode rawSkill :
				rawSkills) {

			SkillData skill =
					convertSkill(
							rawSkill,
							skillIdMap,
							skillEffectIdMap,
							stats
					);


			SkillLocalization localization =
					convertLocalization(
							rawSkill
					);


			skills.add(
					skill
			);


			SkillLocalization previous =
					localizations.put(
							skill.getId(),
							localization
					);


			if (previous != null) {

				throw new IllegalStateException(
						"중복 내부 Skill ID가 있습니다."
						+ " internalId="
						+ skill.getId()
				);
			}
		}


		skills.sort(
				Comparator.comparingInt(
						SkillData::getId
				)
		);


		return new GenerationResult(
				skills,
				localizations,
				stats
		);
	}


	// =========================================================
	// Skill 변환
	// =========================================================

	private static SkillData convertSkill(
			JsonNode raw,
			Map<Long, Integer> skillIdMap,
			Map<Long, Integer> skillEffectIdMap,
			GenerationStats stats)
			throws Exception {

		long swarfarmId =
				requiredPositiveLong(
						raw,
						"id",
						"skill"
				);


		int internalId =
				requireMappedId(
						skillIdMap,
						swarfarmId,
						"skill"
				);


		SkillData skill =
				new SkillData();


		skill.setId(
				internalId
		);


		skill.setSourceIds(
				createSourceIds(
						raw,
						swarfarmId
				)
		);


		skill.setSlot(
				requiredInt(
						raw,
						"slot",
						"skill "
						+ swarfarmId
				)
		);


		Integer cooldown =
				optionalInteger(
						raw,
						"cooltime"
				);


		skill.setCooldown(
				cooldown
		);


		if (cooldown == null) {

			stats.nullCooldownCount++;
		}


		skill.setHits(
				requiredInt(
						raw,
						"hits",
						"skill "
						+ swarfarmId
				)
		);


		skill.setFlags(
				createFlags(
						raw,
						swarfarmId
				)
		);


		skill.setScaling(
				createScaling(
						raw,
						swarfarmId
				)
		);


		List<SkillEffectRefData> effects =
				createEffects(
						raw,
						skillEffectIdMap,
						swarfarmId
				);


		skill.setEffects(
				effects
		);


		stats.effectReferenceCount +=
				effects.size();


		int maxLevel =
				requiredInt(
						raw,
						"max_level",
						"skill "
						+ swarfarmId
				);


		skill.setMaxLevel(
				maxLevel
		);


		List<SkillLevelUpData> levelUps =
				createLevelUps(
						raw,
						swarfarmId
				);


		skill.setLevelUps(
				levelUps
		);


		stats.levelUpCount +=
				levelUps.size();


		if (maxLevel
				!= levelUps.size() + 1) {

			stats.maxLevelMismatchCount++;
		}


		Integer relatedSkillId =
				createRelatedSkillId(
						raw,
						skillIdMap,
						swarfarmId
				);


		skill.setRelatedSkillId(
				relatedSkillId
		);


		if (relatedSkillId != null) {

			stats.relatedSkillReferenceCount++;
		}


		AssetData assets =
				new AssetData();


		assets.setIconKey(
				"skill-"
				+ internalId
		);


		skill.setAssets(
				assets
		);


		return skill;
	}


	// =========================================================
	// Source IDs
	// =========================================================

	private static SourceIds createSourceIds(
			JsonNode raw,
			long swarfarmId) {

		SourceIds sourceIds =
				new SourceIds();


		sourceIds.setSwarfarm(
				Math.toIntExact(
						swarfarmId
				)
		);


		long com2usId =
				requiredPositiveLong(
						raw,
						"com2us_id",
						"skill "
						+ swarfarmId
				);


		sourceIds.setCom2us(
				Math.toIntExact(
						com2usId
				)
		);


		return sourceIds;
	}


	// =========================================================
	// Flags
	// =========================================================

	private static SkillFlags createFlags(
			JsonNode raw,
			long swarfarmId) {

		SkillFlags flags =
				new SkillFlags();


		flags.setPassive(
				requiredBoolean(
						raw,
						"passive",
						"skill "
						+ swarfarmId
				)
		);


		flags.setAoe(
				requiredBoolean(
						raw,
						"aoe",
						"skill "
						+ swarfarmId
				)
		);


		flags.setRandomTarget(
				requiredBoolean(
						raw,
						"random",
						"skill "
						+ swarfarmId
				)
		);


		return flags;
	}


	// =========================================================
	// Scaling
	// =========================================================

	private static SkillScaling createScaling(
			JsonNode raw,
			long swarfarmId)
			throws Exception {

		SkillScaling scaling =
				new SkillScaling();


		String formula =
				optionalString(
						raw,
						"multiplier_formula"
				);


		if (formula != null
				&& formula.isBlank()) {

			formula =
					null;
		}


		scaling.setFormula(
				formula
		);


		String rawExpression =
				requiredString(
						raw,
						"multiplier_formula_raw",
						"skill "
						+ swarfarmId
				);


		List<List<Object>> expression;


		try {

			expression =
					JSON_MAPPER.readValue(
							rawExpression,
							new TypeReference<
									List<List<Object>>>() {}
					);
		}
		catch (Exception e) {

			throw new IllegalStateException(
					"multiplier_formula_raw을 파싱할 수 없습니다."
					+ " skill="
					+ swarfarmId
					+ ", value="
					+ rawExpression,
					e
			);
		}


		scaling.setExpression(
				expression
		);


		scaling.setStats(
				readStringArray(
						raw,
						"scales_with",
						"skill "
						+ swarfarmId
				)
		);


		return scaling;
	}


	// =========================================================
	// Skill Effect Reference
	// =========================================================

	private static List<SkillEffectRefData> createEffects(
			JsonNode raw,
			Map<Long, Integer> skillEffectIdMap,
			long swarfarmId) {

		JsonNode effectsNode =
				requireArray(
						raw,
						"effects",
						"skill "
						+ swarfarmId
				);


		List<SkillEffectRefData> result =
				new ArrayList<>();


		for (JsonNode effectNode :
				effectsNode) {

			JsonNode effectEntityNode =
					effectNode.get(
							"effect"
					);


			Long effectSourceId =
					readRelationId(
							effectEntityNode
					);


			if (effectSourceId == null) {

				throw new IllegalStateException(
						"Skill effect reference ID가 없습니다."
						+ " skill="
						+ swarfarmId
				);
			}


			int internalEffectId =
					requireMappedId(
							skillEffectIdMap,
							effectSourceId,
							"skill effect"
					);


			SkillEffectRefData reference =
					new SkillEffectRefData();


			reference.setEffectId(
					internalEffectId
			);


			reference.setChance(
					optionalInteger(
							effectNode,
							"chance"
					)
			);


			reference.setQuantity(
					optionalInteger(
							effectNode,
							"quantity"
					)
			);


			reference.setFlags(
					createEffectFlags(
							effectNode,
							swarfarmId,
							effectSourceId
					)
			);


			reference.setNote(
					optionalString(
							effectNode,
							"note"
					)
			);


			result.add(
					reference
			);
		}


		return result;
	}


	private static SkillEffectRefFlags createEffectFlags(
			JsonNode node,
			long skillSourceId,
			long effectSourceId) {

		String context =
				"skill "
				+ skillSourceId
				+ " effect "
				+ effectSourceId;


		SkillEffectRefFlags flags =
				new SkillEffectRefFlags();


		flags.setAoe(
				requiredBoolean(
						node,
						"aoe",
						context
				)
		);


		flags.setSingleTarget(
				requiredBoolean(
						node,
						"single_target",
						context
				)
		);


		flags.setSelfEffect(
				requiredBoolean(
						node,
						"self_effect",
						context
				)
		);


		flags.setOnCrit(
				requiredBoolean(
						node,
						"on_crit",
						context
				)
		);


		flags.setOnDeath(
				requiredBoolean(
						node,
						"on_death",
						context
				)
		);


		flags.setRandom(
				requiredBoolean(
						node,
						"random",
						context
				)
		);


		flags.setAllTargets(
				requiredBoolean(
						node,
						"all",
						context
				)
		);


		flags.setSelfHp(
				requiredBoolean(
						node,
						"self_hp",
						context
				)
		);


		flags.setTargetHp(
				requiredBoolean(
						node,
						"target_hp",
						context
				)
		);


		flags.setDamage(
				requiredBoolean(
						node,
						"damage",
						context
				)
		);


		return flags;
	}


	// =========================================================
	// Skill Level Up
	// =========================================================

	private static List<SkillLevelUpData> createLevelUps(
			JsonNode raw,
			long swarfarmId) {

		JsonNode upgradesNode =
				requireArray(
						raw,
						"upgrades",
						"skill "
						+ swarfarmId
				);


		JsonNode descriptionsNode =
				requireArray(
						raw,
						"level_progress_description",
						"skill "
						+ swarfarmId
				);


		if (upgradesNode.size()
				!= descriptionsNode.size()) {

			throw new IllegalStateException(
					"upgrades와 level_progress_description 길이가 다릅니다."
					+ " skill="
					+ swarfarmId
					+ ", upgrades="
					+ upgradesNode.size()
					+ ", descriptions="
					+ descriptionsNode.size()
			);
		}


		List<SkillLevelUpData> result =
				new ArrayList<>();


		for (int index = 0;
				index < upgradesNode.size();
				index++) {

			JsonNode upgradeNode =
					upgradesNode.get(
							index
					);


			SkillLevelUpData levelUp =
					new SkillLevelUpData();


			levelUp.setLevel(
					index + 2
			);


			levelUp.setEffectTemplate(
					requiredString(
							upgradeNode,
							"effect",
							"skill "
							+ swarfarmId
							+ " upgrade "
							+ index
					)
			);


			levelUp.setAmount(
					requiredInt(
							upgradeNode,
							"amount",
							"skill "
							+ swarfarmId
							+ " upgrade "
							+ index
					)
			);


			result.add(
					levelUp
			);
		}


		return result;
	}


	// =========================================================
	// Related Skill
	// =========================================================

	private static Integer createRelatedSkillId(
			JsonNode raw,
			Map<Long, Integer> skillIdMap,
			long swarfarmId) {

		Long relatedSourceId =
				readRelationId(
						raw.get(
								"other_skill"
						)
				);


		if (relatedSourceId == null) {

			return null;
		}


		return requireMappedId(
				skillIdMap,
				relatedSourceId,
				"other_skill referenced by skill "
				+ swarfarmId
		);
	}


	// =========================================================
	// Localization
	// =========================================================

	private static SkillLocalization convertLocalization(
			JsonNode raw) {

		long swarfarmId =
				requiredPositiveLong(
						raw,
						"id",
						"skill localization"
				);


		SkillLocalization localization =
				new SkillLocalization();


		localization.setName(
				requiredString(
						raw,
						"name",
						"skill "
						+ swarfarmId
				)
		);


		/*
		 * raw에는 빈 문자열 description도 존재한다.
		 * null로 임의 변환하지 않고 그대로 보존한다.
		 */
		localization.setDescription(
				requiredPresentString(
						raw,
						"description",
						"skill "
						+ swarfarmId
				)
		);


		localization.setLevelUpDescriptions(
				readStringArray(
						raw,
						"level_progress_description",
						"skill "
						+ swarfarmId
				)
		);


		return localization;
	}


	// =========================================================
	// 배열 보조
	// =========================================================

	private static JsonNode requireArray(
			JsonNode node,
			String field,
			String context) {

		JsonNode valueNode =
				node.get(
						field
				);


		if (valueNode == null
				|| valueNode.isNull()
				|| !valueNode.isArray()) {

			throw new IllegalStateException(
					"필수 배열 값이 없습니다."
					+ " context="
					+ context
					+ ", field="
					+ field
			);
		}


		return valueNode;
	}


	private static List<String> readStringArray(
			JsonNode node,
			String field,
			String context) {

		JsonNode arrayNode =
				requireArray(
						node,
						field,
						context
				);


		List<String> result =
				new ArrayList<>();


		for (JsonNode item :
				arrayNode) {

			if (item == null
					|| item.isNull()) {

				throw new IllegalStateException(
						"문자열 배열에 null 값이 있습니다."
						+ " context="
						+ context
						+ ", field="
						+ field
				);
			}


			result.add(
					item.asString()
			);
		}


		return result;
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
	// Relation ID
	// =========================================================

	private static Long readRelationId(
			JsonNode node) {

		if (node == null
				|| node.isNull()) {

			return null;
		}


		if (node.isObject()) {

			node =
					node.get(
							"id"
					);


			if (node == null
					|| node.isNull()) {

				return null;
			}
		}


		try {

			long value =
					node.asLong();


			if (value <= 0) {

				return null;
			}


			return value;
		}
		catch (Exception e) {

			return null;
		}
	}


	// =========================================================
	// Raw 값 읽기
	// =========================================================

	private static long requiredPositiveLong(
			JsonNode node,
			String field,
			String context) {

		Long value =
				optionalPositiveLong(
						node,
						field
				);


		if (value == null) {

			throw new IllegalStateException(
					"필수 양수 값이 없습니다."
					+ " context="
					+ context
					+ ", field="
					+ field
			);
		}


		return value;
	}


	private static Long optionalPositiveLong(
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


		try {

			long value =
					valueNode.asLong();


			if (value <= 0) {

				return null;
			}


			return value;
		}
		catch (Exception e) {

			return null;
		}
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


	private static Integer optionalInteger(
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


		return valueNode.asInt();
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
	// 파일 확인
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

		private int effectReferenceCount;

		private int levelUpCount;

		private int relatedSkillReferenceCount;

		private int nullCooldownCount;

		private int maxLevelMismatchCount;
	}


	private record GenerationResult(

			List<SkillData> skills,

			Map<Integer, SkillLocalization> localizations,

			GenerationStats stats) {
	}
}