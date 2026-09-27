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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.jim.summoner.data.model.AssetData;
import com.jim.summoner.data.model.AwakeningData;
import com.jim.summoner.data.model.BaseStatsData;
import com.jim.summoner.data.model.FamilyData;
import com.jim.summoner.data.model.MonsterData;
import com.jim.summoner.data.model.MonsterFlags;
import com.jim.summoner.data.model.MonsterLocalization;
import com.jim.summoner.data.model.MonsterSkillRef;
import com.jim.summoner.data.model.SourceIds;
import com.jim.summoner.data.model.StatsData;
import com.jim.summoner.data.type.Archetype;
import com.jim.summoner.data.type.AwakeningStage;
import com.jim.summoner.data.type.Element;
import com.jim.summoner.data.type.MonsterEntityType;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class MonsterCandidateGenerator {

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


		Path curationDirectory =
				projectRoot.resolve(
						Path.of(
								"data-source",
								"curation"
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
				curationDirectory
		);


		Files.createDirectories(
				candidateNormalizedDirectory
		);


		Files.createDirectories(
				candidateLocalizationEnDirectory
		);


		validateRequiredFiles(
				rawDirectory,
				idMapDirectory
		);


		System.out.println(
				"========================================"
		);

		System.out.println(
				"Monster candidate generation start"
		);

		System.out.println(
				"========================================"
		);


		List<JsonNode> rawMonsters =
				readJsonArray(
						rawDirectory.resolve(
								"monsters.json"
						)
				);


		inspectMonsterRawValues(
				rawMonsters
		);


		List<JsonNode> rawSkills =
				readJsonArray(
						rawDirectory.resolve(
								"skills.json"
						)
				);


		Map<Long, Integer> monsterIdMap =
				loadSwarfarmIdMap(
						idMapDirectory.resolve(
								"monsters.json"
						)
				);


		Map<Long, Integer> skillIdMap =
				loadSwarfarmIdMap(
						idMapDirectory.resolve(
								"skills.json"
						)
				);


		Map<Long, Integer> familyIdMap =
				loadSwarfarmIdMap(
						idMapDirectory.resolve(
								"families.json"
						)
				);


		Map<Long, Integer> leaderSkillIdMap =
				loadSwarfarmIdMap(
						idMapDirectory.resolve(
								"leader-skills.json"
						)
				);


		Map<Long, Integer> monsterSourceIdMap =
				loadSwarfarmIdMap(
						idMapDirectory.resolve(
								"monster-sources.json"
						)
				);


		Map<Long, Integer> skillSlotMap =
				buildSkillSlotMap(
						rawSkills
				);


		Map<Integer, MonsterCuration> monsterCurations =
				loadMonsterCurations(
						curationDirectory.resolve(
								"monsters.json"
						)
				);


		validateCurationIds(
				monsterCurations,
				monsterIdMap
		);


		GenerationResult result =
				generate(
						rawMonsters,
						monsterIdMap,
						skillIdMap,
						familyIdMap,
						leaderSkillIdMap,
						monsterSourceIdMap,
						skillSlotMap,
						monsterCurations
				);


		writeJson(
				candidateNormalizedDirectory.resolve(
						"monsters.json"
				),
				result.monsters()
		);


		writeJson(
				candidateNormalizedDirectory.resolve(
						"families.json"
				),
				result.families()
		);


		writeJson(
				candidateLocalizationEnDirectory.resolve(
						"monsters.json"
				),
				result.localizations()
		);


		System.out.println();

		System.out.println(
				"monsters      = "
				+ result.monsters()
						.size()
		);

		System.out.println(
				"families      = "
				+ result.families()
						.size()
		);

		System.out.println(
				"localizations = "
				+ result.localizations()
						.size()
		);

		System.out.println(
				"curations     = "
				+ monsterCurations.size()
		);


		printEntityTypeSummary(
				result.monsters()
		);


		System.out.println();

		System.out.println(
				"========================================"
		);

		System.out.println(
				"Monster candidate generation complete"
		);

		System.out.println(
				"========================================"
		);
	}


	// =========================================================
	// Raw 값 사전 점검
	// =========================================================

	private static void inspectMonsterRawValues(
			List<JsonNode> rawMonsters) {

		Set<String> elements =
				new TreeSet<>();


		Set<String> archetypes =
				new TreeSet<>();


		Set<Integer> awakenLevels =
				new TreeSet<>();


		for (JsonNode monster :
				rawMonsters) {

			JsonNode element =
					monster.get(
							"element"
					);


			if (element != null
					&& !element.isNull()) {

				elements.add(
						element.asString()
				);
			}


			JsonNode archetype =
					monster.get(
							"archetype"
					);


			if (archetype != null
					&& !archetype.isNull()) {

				archetypes.add(
						archetype.asString()
				);
			}


			JsonNode awakenLevel =
					monster.get(
							"awaken_level"
					);


			if (awakenLevel != null
					&& !awakenLevel.isNull()) {

				awakenLevels.add(
						awakenLevel.asInt()
				);
			}
		}


		System.out.println(
				"[RAW VALUES] elements = "
				+ elements
		);

		System.out.println(
				"[RAW VALUES] archetypes = "
				+ archetypes
		);

		System.out.println(
				"[RAW VALUES] awakenLevels = "
				+ awakenLevels
		);
	}


	// =========================================================
	// 전체 변환
	//
	// 1차: raw -> canonical 구조 변환
	// 2차: 전체 관계를 본 뒤 entityType 자동 분류
	// 3차: curation 예외를 마지막에 override
	// =========================================================

	private static GenerationResult generate(
			List<JsonNode> rawMonsters,
			Map<Long, Integer> monsterIdMap,
			Map<Long, Integer> skillIdMap,
			Map<Long, Integer> familyIdMap,
			Map<Long, Integer> leaderSkillIdMap,
			Map<Long, Integer> monsterSourceIdMap,
			Map<Long, Integer> skillSlotMap,
			Map<Integer, MonsterCuration> monsterCurations) {

		List<MonsterData> monsters =
				new ArrayList<>();


		Map<Integer, MonsterLocalization> localizations =
				new TreeMap<>();


		Map<Integer, FamilyData> families =
				new TreeMap<>();


		for (JsonNode rawMonster :
				rawMonsters) {

			MonsterData monster =
					convertMonster(
							rawMonster,
							monsterIdMap,
							skillIdMap,
							familyIdMap,
							leaderSkillIdMap,
							monsterSourceIdMap,
							skillSlotMap
					);


			monsters.add(
					monster
			);


			MonsterLocalization localization =
					convertLocalization(
							rawMonster
					);


			localizations.put(
					monster.getId(),
					localization
			);


			addMonsterToFamily(
					rawMonster,
					monster,
					familyIdMap,
					families
			);
		}


		monsters.sort(
				Comparator.comparingInt(
						MonsterData::getId
				)
		);


		List<FamilyData> familyList =
				new ArrayList<>(
						families.values()
				);


		for (FamilyData family :
				familyList) {

			family.getMonsterIds()
					.sort(
							Integer::compareTo
					);
		}


		familyList.sort(
				Comparator.comparingInt(
						FamilyData::getId
				)
		);


		applyAutomaticEntityTypes(
				monsters
		);


		applyMonsterCurations(
				monsters,
				monsterCurations
		);


		return new GenerationResult(
				monsters,
				familyList,
				localizations
		);
	}


	// =========================================================
	// Entity Type 자동 분류
	// =========================================================

	private static void applyAutomaticEntityTypes(
			List<MonsterData> monsters) {

		Map<Integer, List<MonsterData>> familyMembers =
				buildFamilyMembers(
						monsters
				);


		Set<Integer> transformTargetIds =
				buildTransformTargetIds(
						monsters
				);


		for (MonsterData monster :
				monsters) {

			MonsterEntityType entityType =
					determineAutomaticEntityType(
							monster,
							familyMembers,
							transformTargetIds
					);


			monster.setEntityType(
					entityType
			);
		}
	}


	private static MonsterEntityType determineAutomaticEntityType(
			MonsterData monster,
			Map<Integer, List<MonsterData>> familyMembers,
			Set<Integer> transformTargetIds) {

		/*
		 * Angelmon, Devilmon 등 재료 계열은
		 * obtainable=true일 수도 있기 때문에
		 * 일반 PLAYABLE보다 먼저 분리한다.
		 */
		if (monster.getArchetype()
				== Archetype.MATERIAL) {

			return MonsterEntityType.MATERIAL;
		}


		/*
		 * 현재 실제 데이터에서
		 * archetype NONE / element PURE는
		 * 던전 적, 보스, 수정 등
		 * 비플레이어 특수 엔티티를 나타낸다.
		 *
		 * 세부 NPC/BOSS/OBJECT 분류는
		 * 나중에 필요할 때 curation으로 세분화한다.
		 */
		if (monster.getArchetype()
				== Archetype.NONE
				|| monster.getElement()
				== Element.PURE) {

			return MonsterEntityType.NON_PLAYABLE;
		}


		/*
		 * 현재 획득 가능,
		 * Homunculus,
		 * Fusion Food 등
		 * 플레이어용이라는 강한 신호가 있는 엔티티.
		 */
		if (hasStrongPlayableSignal(
				monster
		)) {

			return MonsterEntityType.PLAYABLE;
		}


		/*
		 * obtainable=false이어도
		 * 전투 중 transform 관계의 양 끝에 있으면
		 * 별도 전투 형태로 본다.
		 */
		if (isTransformLinked(
				monster,
				transformTargetIds
		)) {

			return MonsterEntityType.BATTLE_FORM;
		}


		/*
		 * 자기 자신은 obtainable=false여도
		 * 같은 family 안에 명확한 플레이어용 엔티티가 있으면
		 * 동일 family의 base/관련 형태로 본다.
		 *
		 * Imperfect 계열처럼 예외인 경우는
		 * curation에서 MATERIAL로 override한다.
		 */
		if (familyHasStrongPlayableSignal(
				monster.getFamilyId(),
				familyMembers
		)) {

			return MonsterEntityType.PLAYABLE;
		}


		return MonsterEntityType.UNKNOWN;
	}


	private static boolean hasStrongPlayableSignal(
			MonsterData monster) {

		if (monster.getArchetype()
				== Archetype.MATERIAL
				|| monster.getArchetype()
				== Archetype.NONE
				|| monster.getElement()
				== Element.PURE) {

			return false;
		}


		if (monster.isObtainable()) {

			return true;
		}


		MonsterFlags flags =
				monster.getFlags();


		if (flags == null) {

			return false;
		}


		return flags.isHomunculus()
				|| flags.isFusionFood();
	}


	private static Map<Integer, List<MonsterData>>
			buildFamilyMembers(
					List<MonsterData> monsters) {

		Map<Integer, List<MonsterData>> result =
				new TreeMap<>();


		for (MonsterData monster :
				monsters) {

			Integer familyId =
					monster.getFamilyId();


			if (familyId == null) {

				continue;
			}


			result.computeIfAbsent(
					familyId,
					key -> new ArrayList<>()
			)
			.add(
					monster
			);
		}


		return result;
	}


	private static Set<Integer> buildTransformTargetIds(
			List<MonsterData> monsters) {

		Set<Integer> result =
				new TreeSet<>();


		for (MonsterData monster :
				monsters) {

			Integer transformsToId =
					monster.getTransformsToId();


			if (transformsToId != null) {

				result.add(
						transformsToId
				);
			}
		}


		return result;
	}


	private static boolean isTransformLinked(
			MonsterData monster,
			Set<Integer> transformTargetIds) {

		return monster.getTransformsToId()
				!= null
				|| transformTargetIds.contains(
						monster.getId()
				);
	}


	private static boolean familyHasStrongPlayableSignal(
			Integer familyId,
			Map<Integer, List<MonsterData>> familyMembers) {

		if (familyId == null) {

			return false;
		}


		List<MonsterData> members =
				familyMembers.get(
						familyId
				);


		if (members == null) {

			return false;
		}


		for (MonsterData member :
				members) {

			if (hasStrongPlayableSignal(
					member
			)) {

				return true;
			}
		}


		return false;
	}


	// =========================================================
	// Curation Override
	// =========================================================

	private static void applyMonsterCurations(
			List<MonsterData> monsters,
			Map<Integer, MonsterCuration> monsterCurations) {

		for (MonsterData monster :
				monsters) {

			MonsterCuration curation =
					monsterCurations.get(
							monster.getId()
					);


			if (curation == null
					|| curation.entityType
					== null) {

				continue;
			}


			monster.setEntityType(
					curation.entityType
			);
		}
	}


	// =========================================================
	// Entity Type 결과 요약
	// =========================================================

	private static void printEntityTypeSummary(
			List<MonsterData> monsters) {

		Map<MonsterEntityType, Integer> counts =
				new EnumMap<>(
						MonsterEntityType.class
				);


		for (MonsterEntityType type :
				MonsterEntityType.values()) {

			counts.put(
					type,
					0
			);
		}


		for (MonsterData monster :
				monsters) {

			MonsterEntityType type =
					monster.getEntityType();


			if (type == null) {

				type =
						MonsterEntityType.UNKNOWN;
			}


			counts.put(
					type,
					counts.get(
							type
					) + 1
			);
		}


		System.out.println();

		System.out.println(
				"[ENTITY TYPE]"
		);


		for (MonsterEntityType type :
				MonsterEntityType.values()) {

			System.out.println(
					type
					+ " = "
					+ counts.get(
							type
					)
			);
		}


		int unknownCount =
				counts.get(
						MonsterEntityType.UNKNOWN
				);


		if (unknownCount > 0) {

			System.out.println();

			System.out.println(
					"[REVIEW REQUIRED] UNKNOWN monster count = "
					+ unknownCount
			);


			int printed =
					0;


			for (MonsterData monster :
					monsters) {

				if (monster.getEntityType()
						!= MonsterEntityType.UNKNOWN) {

					continue;
				}


				System.out.println(
						"  internalId="
						+ monster.getId()
						+ ", swarfarmId="
						+ (
								monster.getSourceIds()
								!= null
										? monster.getSourceIds()
												.getSwarfarm()
										: null
						)
						+ ", familyId="
						+ monster.getFamilyId()
				);


				printed++;


				if (printed >= 20) {

					break;
				}
			}
		}
	}


	// =========================================================
	// Monster 변환
	// =========================================================

	private static MonsterData convertMonster(
			JsonNode raw,
			Map<Long, Integer> monsterIdMap,
			Map<Long, Integer> skillIdMap,
			Map<Long, Integer> familyIdMap,
			Map<Long, Integer> leaderSkillIdMap,
			Map<Long, Integer> monsterSourceIdMap,
			Map<Long, Integer> skillSlotMap) {

		long swarfarmId =
				requiredPositiveLong(
						raw,
						"id",
						"monster"
				);


		int internalId =
				requireMappedId(
						monsterIdMap,
						swarfarmId,
						"monster"
				);


		Long familySourceId =
				optionalPositiveLong(
						raw,
						"family_id"
				);


		Integer familyId =
				null;


		if (familySourceId != null) {

			familyId =
					requireMappedId(
							familyIdMap,
							familySourceId,
							"family"
					);
		}


		MonsterData monster =
				new MonsterData();


		monster.setId(
				internalId
		);


		monster.setSourceIds(
				createMonsterSourceIds(
						raw,
						swarfarmId
				)
		);


		monster.setFamilyId(
				familyId
		);


		/*
		 * 실제 entityType은
		 * 모든 몬스터 변환이 끝난 뒤
		 * 관계 기반 2차 분류에서 결정한다.
		 */
		monster.setEntityType(
				MonsterEntityType.UNKNOWN
		);


		monster.setElement(
				convertElement(
						requiredString(
								raw,
								"element",
								"monster "
								+ swarfarmId
						)
				)
		);


		monster.setArchetype(
				convertArchetype(
						requiredString(
								raw,
								"archetype",
								"monster "
								+ swarfarmId
						)
				)
		);


		monster.setBaseStars(
				requiredInt(
						raw,
						"base_stars",
						"monster "
						+ swarfarmId
				)
		);


		monster.setNaturalStars(
				requiredInt(
						raw,
						"natural_stars",
						"monster "
						+ swarfarmId
				)
		);


		monster.setAwakening(
				createAwakening(
						raw,
						monsterIdMap,
						swarfarmId
				)
		);


		monster.setBaseStats(
				createBaseStats(
						raw
				)
		);


		monster.setStats(
				createStats(
						raw
				)
		);


		monster.setSkills(
				createSkillRefs(
						raw,
						skillIdMap,
						skillSlotMap,
						swarfarmId
				)
		);


		monster.setSkillUpsToMax(
				optionalInt(
						raw,
						"skill_ups_to_max",
						0
				)
		);


		monster.setLeaderSkillId(
				createLeaderSkillId(
						raw,
						leaderSkillIdMap,
						swarfarmId
				)
		);


		monster.setObtainable(
				optionalBoolean(
						raw,
						"obtainable",
						false
				)
		);


		monster.setObtainSourceIds(
				createObtainSourceIds(
						raw,
						monsterSourceIdMap,
						swarfarmId
				)
		);


		monster.setFlags(
				createFlags(
						raw
				)
		);


		monster.setTransformsToId(
				createTransformId(
						raw,
						monsterIdMap,
						swarfarmId
				)
		);


		AssetData assets =
				new AssetData();


		assets.setIconKey(
				"monster-"
				+ internalId
		);


		monster.setAssets(
				assets
		);


		return monster;
	}


	// =========================================================
	// Source IDs
	// =========================================================

	private static SourceIds createMonsterSourceIds(
			JsonNode raw,
			long swarfarmId) {

		SourceIds sourceIds =
				new SourceIds();


		sourceIds.setSwarfarm(
				Math.toIntExact(
						swarfarmId
				)
		);


		Long com2usId =
				optionalPositiveLong(
						raw,
						"com2us_id"
				);


		if (com2usId != null) {

			sourceIds.setCom2us(
					Math.toIntExact(
							com2usId
					)
			);
		}


		Long skillGroupId =
				optionalPositiveLong(
						raw,
						"skill_group_id"
				);


		if (skillGroupId != null) {

			sourceIds.setSkillGroup(
					Math.toIntExact(
							skillGroupId
					)
			);
		}


		return sourceIds;
	}


	// =========================================================
	// Awakening
	// =========================================================

	private static AwakeningData createAwakening(
			JsonNode raw,
			Map<Long, Integer> monsterIdMap,
			long monsterSourceId) {

		AwakeningData awakening =
				new AwakeningData();


		int awakenLevel =
				requiredInt(
						raw,
						"awaken_level",
						"monster "
						+ monsterSourceId
				);


		awakening.setStage(
				convertAwakeningStage(
						awakenLevel
				)
		);


		awakening.setCanAwaken(
				optionalBoolean(
						raw,
						"can_awaken",
						false
				)
		);


		Long previousSourceId =
				readRelationId(
						raw.get(
								"awakens_from"
						)
				);


		if (previousSourceId != null) {

			awakening.setPreviousFormId(
					requireMappedId(
							monsterIdMap,
							previousSourceId,
							"awakens_from"
					)
			);
		}


		Long nextSourceId =
				readRelationId(
						raw.get(
								"awakens_to"
						)
				);


		if (nextSourceId != null) {

			awakening.setNextFormId(
					requireMappedId(
							monsterIdMap,
							nextSourceId,
							"awakens_to"
					)
			);
		}


		return awakening;
	}


	// =========================================================
	// Stats
	// =========================================================

	private static BaseStatsData createBaseStats(
			JsonNode raw) {

		BaseStatsData stats =
				new BaseStatsData();


		stats.setHp(
				optionalInteger(
						raw,
						"base_hp"
				)
		);


		stats.setAttack(
				optionalInteger(
						raw,
						"base_attack"
				)
		);


		stats.setDefense(
				optionalInteger(
						raw,
						"base_defense"
				)
		);


		return stats;
	}


	private static StatsData createStats(
			JsonNode raw) {

		StatsData stats =
				new StatsData();


		stats.setHp(
				optionalInteger(
						raw,
						"max_lvl_hp"
				)
		);


		stats.setAttack(
				optionalInteger(
						raw,
						"max_lvl_attack"
				)
		);


		stats.setDefense(
				optionalInteger(
						raw,
						"max_lvl_defense"
				)
		);


		stats.setSpeed(
				optionalInteger(
						raw,
						"speed"
				)
		);


		stats.setCritRate(
				optionalInteger(
						raw,
						"crit_rate"
				)
		);


		stats.setCritDamage(
				optionalInteger(
						raw,
						"crit_damage"
				)
		);


		stats.setResistance(
				optionalInteger(
						raw,
						"resistance"
				)
		);


		stats.setAccuracy(
				optionalInteger(
						raw,
						"accuracy"
				)
		);


		return stats;
	}


	// =========================================================
	// Skills
	// =========================================================

	private static List<MonsterSkillRef> createSkillRefs(
			JsonNode raw,
			Map<Long, Integer> skillIdMap,
			Map<Long, Integer> skillSlotMap,
			long monsterSourceId) {

		JsonNode skillsNode =
				raw.get(
						"skills"
				);


		if (skillsNode == null
				|| skillsNode.isNull()) {

			return List.of();
		}


		if (!skillsNode.isArray()) {

			throw new IllegalStateException(
					"monster skills가 배열이 아닙니다."
					+ " monster="
					+ monsterSourceId
			);
		}


		List<MonsterSkillRef> result =
				new ArrayList<>();


		for (JsonNode skillNode :
				skillsNode) {

			Long skillSourceId =
					readRelationId(
							skillNode
					);


			if (skillSourceId == null) {

				throw new IllegalStateException(
						"유효하지 않은 skill reference입니다."
						+ " monster="
						+ monsterSourceId
				);
			}


			int internalSkillId =
					requireMappedId(
							skillIdMap,
							skillSourceId,
							"skill"
					);


			Integer slot =
					skillSlotMap.get(
							skillSourceId
					);


			if (slot == null) {

				throw new IllegalStateException(
						"skill slot을 찾을 수 없습니다."
						+ " monster="
						+ monsterSourceId
						+ ", skill="
						+ skillSourceId
				);
			}


			MonsterSkillRef skillRef =
					new MonsterSkillRef();


			skillRef.setSkillId(
					internalSkillId
			);


			skillRef.setSlot(
					slot
			);


			result.add(
					skillRef
			);
		}


		return result;
	}


	private static Map<Long, Integer> buildSkillSlotMap(
			List<JsonNode> rawSkills) {

		Map<Long, Integer> result =
				new LinkedHashMap<>();


		for (JsonNode skill :
				rawSkills) {

			long skillId =
					requiredPositiveLong(
							skill,
							"id",
							"skill"
					);


			int slot =
					requiredInt(
							skill,
							"slot",
							"skill "
							+ skillId
					);


			Integer previous =
					result.put(
							skillId,
							slot
					);


			if (previous != null) {

				throw new IllegalStateException(
						"중복 skill id가 있습니다."
						+ " skill="
						+ skillId
				);
			}
		}


		return result;
	}


	// =========================================================
	// Leader Skill
	// =========================================================

	private static Integer createLeaderSkillId(
			JsonNode raw,
			Map<Long, Integer> leaderSkillIdMap,
			long monsterSourceId) {

		Long sourceId =
				readRelationId(
						raw.get(
								"leader_skill"
						)
				);


		if (sourceId == null) {

			return null;
		}


		return requireMappedId(
				leaderSkillIdMap,
				sourceId,
				"leader skill referenced by monster "
				+ monsterSourceId
		);
	}


	// =========================================================
	// Monster Source
	// =========================================================

	private static List<Integer> createObtainSourceIds(
			JsonNode raw,
			Map<Long, Integer> monsterSourceIdMap,
			long monsterSourceId) {

		JsonNode sourcesNode =
				raw.get(
						"source"
				);


		if (sourcesNode == null
				|| sourcesNode.isNull()) {

			return List.of();
		}


		if (!sourcesNode.isArray()) {

			throw new IllegalStateException(
					"monster source가 배열이 아닙니다."
					+ " monster="
					+ monsterSourceId
			);
		}


		List<Integer> result =
				new ArrayList<>();


		for (JsonNode sourceNode :
				sourcesNode) {

			Long sourceId =
					readRelationId(
							sourceNode
					);


			if (sourceId == null) {

				throw new IllegalStateException(
						"유효하지 않은 monster source입니다."
						+ " monster="
						+ monsterSourceId
				);
			}


			result.add(
					requireMappedId(
							monsterSourceIdMap,
							sourceId,
							"monster source"
					)
			);
		}


		return result;
	}


	// =========================================================
	// Flags
	// =========================================================

	private static MonsterFlags createFlags(
			JsonNode raw) {

		MonsterFlags flags =
				new MonsterFlags();


		flags.setFusionFood(
				optionalBoolean(
						raw,
						"fusion_food",
						false
				)
		);


		flags.setHomunculus(
				optionalBoolean(
						raw,
						"homunculus",
						false
				)
		);


		return flags;
	}


	// =========================================================
	// Transform
	// =========================================================

	private static Integer createTransformId(
			JsonNode raw,
			Map<Long, Integer> monsterIdMap,
			long monsterSourceId) {

		Long transformSourceId =
				readRelationId(
						raw.get(
								"transforms_to"
						)
				);


		if (transformSourceId == null) {

			return null;
		}


		return requireMappedId(
				monsterIdMap,
				transformSourceId,
				"transforms_to referenced by monster "
				+ monsterSourceId
		);
	}


	// =========================================================
	// Localization
	// =========================================================

	private static MonsterLocalization convertLocalization(
			JsonNode raw) {

		MonsterLocalization localization =
				new MonsterLocalization();


		localization.setName(
				requiredString(
						raw,
						"name",
						"monster localization"
				)
		);


		String awakeningBonus =
				optionalString(
						raw,
						"awaken_bonus"
				);


		if (awakeningBonus != null
				&& !awakeningBonus.isBlank()) {

			localization.setAwakeningBonus(
					awakeningBonus
			);
		}


		/*
		 * SWARFARM monster raw 데이터만으로
		 * familyName을 안전하게 결정할 수 없으므로
		 * 추측하지 않는다.
		 */
		localization.setFamilyName(
				null
		);


		return localization;
	}


	// =========================================================
	// Family
	// =========================================================

	private static void addMonsterToFamily(
			JsonNode rawMonster,
			MonsterData monster,
			Map<Long, Integer> familyIdMap,
			Map<Integer, FamilyData> families) {

		Long familySourceId =
				optionalPositiveLong(
						rawMonster,
						"family_id"
				);


		if (familySourceId == null) {

			return;
		}


		int internalFamilyId =
				requireMappedId(
						familyIdMap,
						familySourceId,
						"family"
				);


		FamilyData family =
				families.computeIfAbsent(
						internalFamilyId,
						key -> {

							FamilyData created =
									new FamilyData();


							created.setId(
									key
							);


							SourceIds sourceIds =
									new SourceIds();


							sourceIds.setSwarfarm(
									Math.toIntExact(
											familySourceId
									)
							);


							created.setSourceIds(
									sourceIds
							);


							return created;
						}
				);


		family.getMonsterIds()
				.add(
						monster.getId()
				);
	}


	// =========================================================
	// Curation
	// =========================================================

	private static Map<Integer, MonsterCuration>
			loadMonsterCurations(
					Path file)
					throws Exception {

		if (!Files.exists(
				file
		)) {

			return Map.of();
		}


		try (InputStream inputStream =
				Files.newInputStream(
						file
				)) {

			Map<String, MonsterCuration> raw =
					JSON_MAPPER.readValue(
							inputStream,
							new TypeReference<
									Map<String, MonsterCuration>>() {}
					);


			Map<Integer, MonsterCuration> result =
					new LinkedHashMap<>();


			for (Map.Entry<String, MonsterCuration> entry :
					raw.entrySet()) {

				int internalId;


				try {

					internalId =
							Integer.parseInt(
									entry.getKey()
							);
				}
				catch (NumberFormatException e) {

					throw new IllegalStateException(
							"curation monster ID가 숫자가 아닙니다."
							+ " id="
							+ entry.getKey(),
							e
					);
				}


				result.put(
						internalId,
						entry.getValue()
				);
			}


			return result;
		}
	}


	private static void validateCurationIds(
			Map<Integer, MonsterCuration> monsterCurations,
			Map<Long, Integer> monsterIdMap) {

		Set<Integer> validInternalIds =
				new TreeSet<>(
						monsterIdMap.values()
				);


		for (Integer internalId :
				monsterCurations.keySet()) {

			if (!validInternalIds.contains(
					internalId
			)) {

				throw new IllegalStateException(
						"curation에 존재하지 않는 내부 Monster ID가 있습니다."
						+ " internalId="
						+ internalId
				);
			}
		}
	}


	// =========================================================
	// Enum 변환
	// =========================================================

	private static Element convertElement(
			String value) {

		return switch (
				value.trim()
						.toLowerCase(
								Locale.ROOT
						)
		) {

		case "fire" ->
			Element.FIRE;

		case "water" ->
			Element.WATER;

		case "wind" ->
			Element.WIND;

		case "light" ->
			Element.LIGHT;

		case "dark" ->
			Element.DARK;

		case "pure" ->
			Element.PURE;

		default ->
			throw new IllegalStateException(
					"알 수 없는 element 값입니다. value="
					+ value
			);
		};
	}


	private static Archetype convertArchetype(
			String value) {

		return switch (
				value.trim()
						.toLowerCase(
								Locale.ROOT
						)
		) {

		case "attack" ->
			Archetype.ATTACK;

		case "defense" ->
			Archetype.DEFENSE;

		case "hp" ->
			Archetype.HP;

		case "support" ->
			Archetype.SUPPORT;

		case "material" ->
			Archetype.MATERIAL;

		case "none" ->
			Archetype.NONE;

		default ->
			throw new IllegalStateException(
					"알 수 없는 archetype 값입니다. value="
					+ value
			);
		};
	}


	private static AwakeningStage convertAwakeningStage(
			int awakenLevel) {

		return switch (
				awakenLevel
		) {

		case 0 ->
			AwakeningStage.BASE;

		case 1 ->
			AwakeningStage.AWAKENED;

		case 2 ->
			AwakeningStage.SECOND_AWAKENED;

		default ->
			throw new IllegalStateException(
					"알 수 없는 awaken_level 값입니다. value="
					+ awakenLevel
			);
		};
	}


	// =========================================================
	// ID map 읽기
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

			result.put(
					Long.parseLong(
							entry.getKey()
					),
					entry.getValue()
			);
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
	// JSON relation ID
	//
	// 숫자 형태와 {"id": ...} 형태를 둘 다 지원한다.
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
	// Raw JSON 읽기
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


	private static int optionalInt(
			JsonNode node,
			String field,
			int defaultValue) {

		JsonNode valueNode =
				node.get(
						field
				);


		if (valueNode == null
				|| valueNode.isNull()) {

			return defaultValue;
		}


		return valueNode.asInt();
	}


	private static boolean optionalBoolean(
			JsonNode node,
			String field,
			boolean defaultValue) {

		JsonNode valueNode =
				node.get(
						field
				);


		if (valueNode == null
				|| valueNode.isNull()) {

			return defaultValue;
		}


		return valueNode.asBoolean();
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
	// 필수 파일 확인
	// =========================================================

	private static void validateRequiredFiles(
			Path rawDirectory,
			Path idMapDirectory) {

		List<Path> requiredFiles =
				List.of(

						rawDirectory.resolve(
								"monsters.json"
						),

						rawDirectory.resolve(
								"skills.json"
						),

						idMapDirectory.resolve(
								"monsters.json"
						),

						idMapDirectory.resolve(
								"skills.json"
						),

						idMapDirectory.resolve(
								"families.json"
						),

						idMapDirectory.resolve(
								"leader-skills.json"
						),

						idMapDirectory.resolve(
								"monster-sources.json"
						)
				);


		for (Path file :
				requiredFiles) {

			if (!Files.exists(
					file
			)) {

				throw new IllegalStateException(
						"필수 파일이 없습니다. file="
						+ file.toAbsolutePath()
				);
			}
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


	public static class MonsterCuration {

		public MonsterEntityType entityType;

		public String note;
	}


	private record GenerationResult(

			List<MonsterData> monsters,

			List<FamilyData> families,

			Map<Integer, MonsterLocalization> localizations) {
	}
}