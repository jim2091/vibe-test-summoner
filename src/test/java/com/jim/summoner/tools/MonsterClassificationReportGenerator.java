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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.jim.summoner.data.model.BaseStatsData;
import com.jim.summoner.data.model.MonsterData;
import com.jim.summoner.data.model.MonsterFlags;
import com.jim.summoner.data.model.MonsterLocalization;
import com.jim.summoner.data.model.SourceIds;
import com.jim.summoner.data.model.StatsData;
import com.jim.summoner.data.type.Archetype;
import com.jim.summoner.data.type.Element;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

public class MonsterClassificationReportGenerator {

	private static final JsonMapper JSON_MAPPER =
			new JsonMapper();


	public static void main(String[] args)
			throws Exception {

		Path projectRoot =
				resolveProjectRoot();


		Path candidateDirectory =
				projectRoot.resolve(
						Path.of(
								"data-source",
								"candidate"
						)
				);


		Path monstersFile =
				candidateDirectory.resolve(
						Path.of(
								"normalized",
								"monsters.json"
						)
				);


		Path localizationFile =
				candidateDirectory.resolve(
						Path.of(
								"localization",
								"en",
								"monsters.json"
						)
				);


		Path reportDirectory =
				projectRoot.resolve(
						Path.of(
								"data-source",
								"reports"
						)
				);


		Path classificationReportFile =
				reportDirectory.resolve(
						"monster-classification.csv"
				);


		Path unresolvedFamilyReportFile =
				reportDirectory.resolve(
						"unresolved-family-review.csv"
				);


		validateRequiredFile(
				monstersFile
		);


		validateRequiredFile(
				localizationFile
		);


		Files.createDirectories(
				reportDirectory
		);


		System.out.println(
				"========================================"
		);

		System.out.println(
				"Monster classification report start"
		);

		System.out.println(
				"========================================"
		);


		List<MonsterData> monsters =
				readMonsters(
						monstersFile
				);


		Map<Integer, MonsterLocalization> localizations =
				readLocalizations(
						localizationFile
				);


		Map<Integer, MonsterData> monsterById =
				buildMonsterById(
						monsters
				);


		Map<Integer, List<MonsterData>> familyMembers =
				buildFamilyMembers(
						monsters
				);


		Map<Integer, List<Integer>> transformedFromMap =
				buildTransformedFromMap(
						monsters
				);


		List<ReviewRow> reviewRows =
				new ArrayList<>();


		for (MonsterData monster :
				monsters) {

			MonsterLocalization localization =
					localizations.get(
							monster.getId()
					);


			reviewRows.add(
					createReviewRow(
							monster,
							localization,
							familyMembers,
							monsterById,
							transformedFromMap,
							localizations
					)
			);
		}


		reviewRows.sort(
				Comparator
						.comparing(
								ReviewRow::group
						)
						.thenComparingInt(
								ReviewRow::internalId
						)
		);


		Map<Integer, ReviewRow> reviewRowById =
				buildReviewRowById(
						reviewRows
				);


		List<UnresolvedFamilyRow> unresolvedFamilyRows =
				createUnresolvedFamilyRows(
						reviewRows,
						familyMembers,
						localizations,
						reviewRowById,
						transformedFromMap
				);


		writeClassificationCsv(
				classificationReportFile,
				reviewRows
		);


		writeUnresolvedFamilyCsv(
				unresolvedFamilyReportFile,
				unresolvedFamilyRows
		);


		printSummary(
				reviewRows,
				unresolvedFamilyRows
		);


		System.out.println();

		System.out.println(
				"[SAVED] "
				+ classificationReportFile.toAbsolutePath()
		);


		System.out.println(
				"[SAVED] "
				+ unresolvedFamilyReportFile.toAbsolutePath()
		);


		System.out.println();

		System.out.println(
				"========================================"
		);

		System.out.println(
				"Monster classification report complete"
		);

		System.out.println(
				"========================================"
		);
	}


	// =========================================================
	// Monster ID Index
	// =========================================================

	private static Map<Integer, MonsterData> buildMonsterById(
			List<MonsterData> monsters) {

		Map<Integer, MonsterData> result =
				new LinkedHashMap<>();


		for (MonsterData monster :
				monsters) {

			MonsterData previous =
					result.put(
							monster.getId(),
							monster
					);


			if (previous != null) {

				throw new IllegalStateException(
						"중복 internal monster ID가 있습니다."
						+ " id="
						+ monster.getId()
				);
			}
		}


		return result;
	}


	// =========================================================
	// Family 구성
	// =========================================================

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


			result
					.computeIfAbsent(
							familyId,
							key -> new ArrayList<>()
					)
					.add(
							monster
					);
		}


		for (List<MonsterData> members :
				result.values()) {

			members.sort(
					Comparator.comparingInt(
							MonsterData::getId
					)
			);
		}


		return result;
	}


	// =========================================================
	// Transform 역방향 관계
	//
	// A.transformsToId = B 라면
	// B의 transformedFrom 목록에 A를 기록한다.
	// =========================================================

	private static Map<Integer, List<Integer>>
			buildTransformedFromMap(
					List<MonsterData> monsters) {

		Map<Integer, List<Integer>> result =
				new TreeMap<>();


		for (MonsterData monster :
				monsters) {

			Integer transformsToId =
					monster.getTransformsToId();


			if (transformsToId == null) {
				continue;
			}


			result
					.computeIfAbsent(
							transformsToId,
							key -> new ArrayList<>()
					)
					.add(
							monster.getId()
					);
		}


		for (List<Integer> ids :
				result.values()) {

			ids.sort(
					Integer::compareTo
			);
		}


		return result;
	}


	// =========================================================
	// ReviewRow Index
	// =========================================================

	private static Map<Integer, ReviewRow> buildReviewRowById(
			List<ReviewRow> rows) {

		Map<Integer, ReviewRow> result =
				new LinkedHashMap<>();


		for (ReviewRow row :
				rows) {

			result.put(
					row.internalId(),
					row
			);
		}


		return result;
	}


	// =========================================================
	// 개별 Monster 분류
	// =========================================================

	private static ReviewRow createReviewRow(
			MonsterData monster,
			MonsterLocalization localization,
			Map<Integer, List<MonsterData>> familyMembers,
			Map<Integer, MonsterData> monsterById,
			Map<Integer, List<Integer>> transformedFromMap,
			Map<Integer, MonsterLocalization> localizations) {

		List<String> reasons =
				new ArrayList<>();


		List<String> missingStats =
				findMissingStats(
						monster
				);


		MonsterFlags flags =
				monster.getFlags();


		boolean homunculus =
				flags != null
				&& flags.isHomunculus();


		boolean fusionFood =
				flags != null
				&& flags.isFusionFood();


		boolean materialSignal =
				monster.getArchetype()
				== Archetype.MATERIAL;


		boolean specialSignal =
				false;


		if (materialSignal) {

			reasons.add(
					"MATERIAL_ARCHETYPE"
			);
		}


		if (monster.getElement()
				== Element.PURE) {

			specialSignal =
					true;

			reasons.add(
					"ELEMENT_PURE"
			);
		}


		if (monster.getArchetype()
				== Archetype.NONE) {

			specialSignal =
					true;

			reasons.add(
					"ARCHETYPE_NONE"
			);
		}


		if (monster.getFamilyId()
				== null) {

			specialSignal =
					true;

			reasons.add(
					"NO_FAMILY"
			);
		}


		if (!missingStats.isEmpty()) {

			specialSignal =
					true;

			reasons.add(
					"MISSING_STATS"
			);
		}


		if (monster.getSkills() == null
				|| monster.getSkills().isEmpty()) {

			specialSignal =
					true;

			reasons.add(
					"NO_SKILLS"
			);
		}


		boolean strongPlayableSignal =
				hasStrongPlayableSignal(
						monster
				);


		if (monster.isObtainable()) {

			reasons.add(
					"OBTAINABLE"
			);
		}


		if (homunculus) {

			reasons.add(
					"HOMUNCULUS"
			);
		}


		if (fusionFood) {

			reasons.add(
					"FUSION_FOOD"
			);
		}


		Integer transformsToId =
				monster.getTransformsToId();


		MonsterData transformsToMonster =
				transformsToId != null
						? monsterById.get(
								transformsToId
						)
						: null;


		String transformsToName =
				transformsToMonster != null
						? getLocalizationName(
								transformsToMonster.getId(),
								localizations
						)
						: null;


		List<Integer> transformedFromIds =
				transformedFromMap.getOrDefault(
						monster.getId(),
						List.of()
				);


		boolean transformLinked =
				transformsToId != null
				|| !transformedFromIds.isEmpty();


		if (transformsToId != null) {

			reasons.add(
					"TRANSFORMS_TO"
			);
		}


		if (!transformedFromIds.isEmpty()) {

			reasons.add(
					"TRANSFORMED_FROM"
			);
		}


		String transformedFromIdText =
				joinIntegerList(
						transformedFromIds
				);


		String transformedFromNameText =
				joinMonsterNames(
						transformedFromIds,
						localizations
				);


		boolean familyHasPlayableSignal =
				hasFamilyPlayableSignal(
						monster.getFamilyId(),
						familyMembers
				);


		ReviewGroup group;


		/*
		 * 1순위
		 *
		 * 재료 몬스터는 obtainable=true일 수 있으므로
		 * playable보다 먼저 분리한다.
		 */
		if (materialSignal) {

			group =
					ReviewGroup.MATERIAL_CANDIDATE;
		}

		/*
		 * 2순위
		 *
		 * PURE / NONE / stats 누락 / family 없음 등의
		 * 특수 개체 신호가 있는 데이터.
		 *
		 * Boss, NPC, Object 등이 주로 이쪽에서 검토된다.
		 */
		else if (specialSignal) {

			group =
					ReviewGroup.SPECIAL_ENTITY_CANDIDATE;
		}

		/*
		 * 3순위
		 *
		 * 실제 획득 가능 등의 강한 playable 신호가 있다면
		 * transforms 관계가 있더라도 본체로 우선 취급한다.
		 */
		else if (strongPlayableSignal) {

			group =
					ReviewGroup.PLAYABLE_CANDIDATE;
		}

		/*
		 * 4순위
		 *
		 * 자기 자신은 playable 신호가 없지만
		 * transforms 관계의 양 끝 중 하나라면
		 * 전투 중 변신/상태 엔티티 후보로 본다.
		 *
		 * 여기서 확정하지 않고 후보로만 둔다.
		 */
		else if (transformLinked) {

			group =
					ReviewGroup.BATTLE_FORM_CANDIDATE;

			reasons.add(
					"TRANSFORM_LINKED"
			);
		}

		/*
		 * 5순위
		 *
		 * 자기 자신은 획득 불가지만,
		 * 같은 family 안에 playable 신호가 존재.
		 */
		else if (familyHasPlayableSignal) {

			group =
					ReviewGroup.RELATED_FORM_REVIEW;

			reasons.add(
					"FAMILY_HAS_PLAYABLE_SIGNAL"
			);
		}

		/*
		 * 6순위
		 *
		 * family 전체에서도 playable 신호를 찾지 못했고
		 * transform 관계도 확인되지 않은 경우.
		 *
		 * 사람이 family 단위로 검토한다.
		 */
		else {

			group =
					ReviewGroup.UNRESOLVED_FAMILY_REVIEW;

			reasons.add(
					"NO_PLAYABLE_SIGNAL_IN_FAMILY"
			);
		}


		List<MonsterData> members =
				getFamilyMembers(
						monster.getFamilyId(),
						familyMembers
				);


		int familyMemberCount =
				members.size();


		int familyObtainableCount =
				countObtainableMembers(
						members
				);


		SourceIds sourceIds =
				monster.getSourceIds();


		Integer swarfarmId =
				sourceIds != null
						? sourceIds.getSwarfarm()
						: null;


		Integer com2usId =
				sourceIds != null
						? sourceIds.getCom2us()
						: null;


		String name =
				localization != null
						? localization.getName()
						: null;


		String awakeningStage =
				monster.getAwakening() != null
						&& monster.getAwakening()
								.getStage() != null
						? monster.getAwakening()
								.getStage()
								.name()
						: null;


		return new ReviewRow(

				group,

				monster.getId(),

				swarfarmId,

				com2usId,

				name,

				monster.getElement() != null
						? monster.getElement()
								.name()
						: null,

				monster.getArchetype() != null
						? monster.getArchetype()
								.name()
						: null,

				awakeningStage,

				monster.isObtainable(),

				homunculus,

				fusionFood,

				monster.getFamilyId(),

				familyMemberCount,

				familyObtainableCount,

				familyHasPlayableSignal,

				monster.getSkills() != null
						? monster.getSkills()
								.size()
						: 0,

				String.join(
						"|",
						missingStats
				),

				transformsToId,

				transformsToName,

				transformedFromIdText,

				transformedFromNameText,

				transformLinked,

				String.join(
						"|",
						reasons
				)
		);
	}


	// =========================================================
	// Strong Playable Signal
	// =========================================================

	private static boolean hasStrongPlayableSignal(
			MonsterData monster) {

		/*
		 * MATERIAL이나 명시적인 특수 archetype은
		 * obtainable=true여도 일반 playable 판정에서 제외한다.
		 */
		if (monster.getArchetype()
				== Archetype.MATERIAL) {

			return false;
		}


		if (monster.getArchetype()
				== Archetype.NONE) {

			return false;
		}


		if (monster.getElement()
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


	// =========================================================
	// Family playable 신호
	// =========================================================

	private static boolean hasFamilyPlayableSignal(
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


	private static List<MonsterData> getFamilyMembers(
			Integer familyId,
			Map<Integer, List<MonsterData>> familyMembers) {

		if (familyId == null) {
			return List.of();
		}


		List<MonsterData> members =
				familyMembers.get(
						familyId
				);


		if (members == null) {
			return List.of();
		}


		return members;
	}


	private static int countObtainableMembers(
			List<MonsterData> members) {

		int count =
				0;


		for (MonsterData member :
				members) {

			if (member.isObtainable()) {
				count++;
			}
		}


		return count;
	}


	// =========================================================
	// 누락 Stats 확인
	// =========================================================

	private static List<String> findMissingStats(
			MonsterData monster) {

		List<String> result =
				new ArrayList<>();


		BaseStatsData baseStats =
				monster.getBaseStats();


		if (baseStats == null) {

			result.add(
					"baseStats"
			);
		}
		else {

			if (baseStats.getHp() == null) {

				result.add(
						"base_hp"
				);
			}


			if (baseStats.getAttack() == null) {

				result.add(
						"base_attack"
				);
			}


			if (baseStats.getDefense() == null) {

				result.add(
						"base_defense"
				);
			}
		}


		StatsData stats =
				monster.getStats();


		if (stats == null) {

			result.add(
					"stats"
			);

			return result;
		}


		if (stats.getHp() == null) {

			result.add(
					"max_hp"
			);
		}


		if (stats.getAttack() == null) {

			result.add(
					"max_attack"
			);
		}


		if (stats.getDefense() == null) {

			result.add(
					"max_defense"
			);
		}


		if (stats.getSpeed() == null) {

			result.add(
					"speed"
			);
		}


		if (stats.getCritRate() == null) {

			result.add(
					"crit_rate"
			);
		}


		if (stats.getCritDamage() == null) {

			result.add(
					"crit_damage"
			);
		}


		if (stats.getResistance() == null) {

			result.add(
					"resistance"
			);
		}


		if (stats.getAccuracy() == null) {

			result.add(
					"accuracy"
			);
		}


		return result;
	}


	// =========================================================
	// 이름 보조
	// =========================================================

	private static String getLocalizationName(
			int monsterId,
			Map<Integer, MonsterLocalization> localizations) {

		MonsterLocalization localization =
				localizations.get(
						monsterId
				);


		if (localization == null) {
			return null;
		}


		return localization.getName();
	}


	private static String joinMonsterNames(
			List<Integer> monsterIds,
			Map<Integer, MonsterLocalization> localizations) {

		List<String> names =
				new ArrayList<>();


		for (Integer monsterId :
				monsterIds) {

			String name =
					getLocalizationName(
							monsterId,
							localizations
					);


			if (name == null
					|| name.isBlank()) {

				names.add(
						String.valueOf(
								monsterId
						)
				);
			}
			else {

				names.add(
						name
				);
			}
		}


		return String.join(
				"|",
				names
		);
	}


	private static String joinIntegerList(
			List<Integer> values) {

		List<String> result =
				new ArrayList<>();


		for (Integer value :
				values) {

			result.add(
					String.valueOf(
							value
					)
			);
		}


		return String.join(
				"|",
				result
		);
	}


	// =========================================================
	// 미해결 Family 보고서
	// =========================================================

	private static List<UnresolvedFamilyRow>
			createUnresolvedFamilyRows(
					List<ReviewRow> reviewRows,
					Map<Integer, List<MonsterData>> familyMembers,
					Map<Integer, MonsterLocalization> localizations,
					Map<Integer, ReviewRow> reviewRowById,
					Map<Integer, List<Integer>> transformedFromMap) {

		Set<Integer> unresolvedFamilyIds =
				new LinkedHashSet<>();


		Map<Integer, Integer> unresolvedCounts =
				new LinkedHashMap<>();


		for (ReviewRow row :
				reviewRows) {

			if (row.group()
					!= ReviewGroup.UNRESOLVED_FAMILY_REVIEW) {

				continue;
			}


			if (row.familyId()
					== null) {

				continue;
			}


			unresolvedFamilyIds.add(
					row.familyId()
			);


			unresolvedCounts.compute(
					row.familyId(),
					(key, value) ->
							value == null
									? 1
									: value + 1
			);
		}


		List<UnresolvedFamilyRow> result =
				new ArrayList<>();


		for (Integer familyId :
				unresolvedFamilyIds) {

			List<MonsterData> members =
					familyMembers.get(
							familyId
					);


			if (members == null) {
				continue;
			}


			List<String> memberDescriptions =
					new ArrayList<>();


			int battleFormCandidateCount =
					0;


			int playableCandidateCount =
					0;


			int relatedFormReviewCount =
					0;


			for (MonsterData member :
					members) {

				MonsterLocalization localization =
						localizations.get(
								member.getId()
						);


				String name =
						localization != null
								? localization.getName()
								: null;


				String element =
						member.getElement() != null
								? member.getElement()
										.name()
								: "";


				String archetype =
						member.getArchetype() != null
								? member.getArchetype()
										.name()
								: "";


				String awakeningStage =
						member.getAwakening() != null
								&& member.getAwakening()
										.getStage() != null
								? member.getAwakening()
										.getStage()
										.name()
								: "";


				SourceIds sourceIds =
						member.getSourceIds();


				Integer swarfarmId =
						sourceIds != null
								? sourceIds.getSwarfarm()
								: null;


				ReviewRow memberReview =
						reviewRowById.get(
								member.getId()
						);


				String reviewGroup =
						memberReview != null
								? memberReview.group()
										.name()
								: "";


				if (memberReview != null) {

					if (memberReview.group()
							== ReviewGroup.BATTLE_FORM_CANDIDATE) {

						battleFormCandidateCount++;
					}


					if (memberReview.group()
							== ReviewGroup.PLAYABLE_CANDIDATE) {

						playableCandidateCount++;
					}


					if (memberReview.group()
							== ReviewGroup.RELATED_FORM_REVIEW) {

						relatedFormReviewCount++;
					}
				}


				Integer transformsToId =
						member.getTransformsToId();


				List<Integer> transformedFromIds =
						transformedFromMap.getOrDefault(
								member.getId(),
								List.of()
						);


				String description =
						member.getId()
						+ ":"
						+ safeText(
								name
						)
						+ "[sw="
						+ safeText(
								swarfarmId
						)
						+ ","
						+ element
						+ ","
						+ archetype
						+ ","
						+ awakeningStage
						+ ",obtainable="
						+ member.isObtainable()
						+ ",group="
						+ reviewGroup
						+ ",to="
						+ safeText(
								transformsToId
						)
						+ ",from="
						+ joinIntegerList(
								transformedFromIds
						)
						+ "]";


				memberDescriptions.add(
						description
				);
			}


			result.add(
					new UnresolvedFamilyRow(

							familyId,

							unresolvedCounts.getOrDefault(
									familyId,
									0
							),

							members.size(),

							countObtainableMembers(
									members
							),

							playableCandidateCount,

							battleFormCandidateCount,

							relatedFormReviewCount,

							String.join(
									" | ",
									memberDescriptions
							)
					)
			);
		}


		result.sort(
				Comparator.comparingInt(
						UnresolvedFamilyRow::familyId
				)
		);


		return result;
	}


	private static String safeText(
			Object value) {

		return value == null
				? ""
				: String.valueOf(
						value
				);
	}


	// =========================================================
	// 메인 CSV 저장
	// =========================================================

	private static void writeClassificationCsv(
			Path reportFile,
			List<ReviewRow> rows)
			throws Exception {

		StringBuilder builder =
				new StringBuilder();


		appendCsvLine(
				builder,
				List.of(
						"reviewGroup",
						"internalId",
						"swarfarmId",
						"com2usId",
						"name",
						"element",
						"archetype",
						"awakeningStage",
						"obtainable",
						"homunculus",
						"fusionFood",
						"familyId",
						"familyMemberCount",
						"familyObtainableCount",
						"familyHasPlayableSignal",
						"skillCount",
						"missingStats",
						"transformsToId",
						"transformsToName",
						"transformedFromIds",
						"transformedFromNames",
						"transformLinked",
						"reasons"
				)
		);


		for (ReviewRow row :
				rows) {

			appendCsvLine(
					builder,
					List.of(
							row.group()
									.name(),

							row.internalId(),

							nullable(
									row.swarfarmId()
							),

							nullable(
									row.com2usId()
							),

							nullable(
									row.name()
							),

							nullable(
									row.element()
							),

							nullable(
									row.archetype()
							),

							nullable(
									row.awakeningStage()
							),

							row.obtainable(),

							row.homunculus(),

							row.fusionFood(),

							nullable(
									row.familyId()
							),

							row.familyMemberCount(),

							row.familyObtainableCount(),

							row.familyHasPlayableSignal(),

							row.skillCount(),

							nullable(
									row.missingStats()
							),

							nullable(
									row.transformsToId()
							),

							nullable(
									row.transformsToName()
							),

							nullable(
									row.transformedFromIds()
							),

							nullable(
									row.transformedFromNames()
							),

							row.transformLinked(),

							nullable(
									row.reasons()
							)
					)
			);
		}


		writeUtf8BomFile(
				reportFile,
				builder.toString()
		);
	}


	// =========================================================
	// 미해결 Family CSV 저장
	// =========================================================

	private static void writeUnresolvedFamilyCsv(
			Path reportFile,
			List<UnresolvedFamilyRow> rows)
			throws Exception {

		StringBuilder builder =
				new StringBuilder();


		appendCsvLine(
				builder,
				List.of(
						"familyId",
						"unresolvedMemberCount",
						"totalFamilyMemberCount",
						"obtainableMemberCount",
						"playableCandidateCount",
						"battleFormCandidateCount",
						"relatedFormReviewCount",
						"members"
				)
		);


		for (UnresolvedFamilyRow row :
				rows) {

			appendCsvLine(
					builder,
					List.of(
							row.familyId(),
							row.unresolvedMemberCount(),
							row.totalFamilyMemberCount(),
							row.obtainableMemberCount(),
							row.playableCandidateCount(),
							row.battleFormCandidateCount(),
							row.relatedFormReviewCount(),
							row.members()
					)
			);
		}


		writeUtf8BomFile(
				reportFile,
				builder.toString()
		);
	}


	// =========================================================
	// CSV 공통
	// =========================================================

	private static void appendCsvLine(
			StringBuilder builder,
			List<?> values) {

		for (int i = 0;
				i < values.size();
				i++) {

			if (i > 0) {

				builder.append(',');
			}


			appendCsvValue(
					builder,
					values.get(
							i
					)
			);
		}


		builder.append(
				System.lineSeparator()
		);
	}


	private static void appendCsvValue(
			StringBuilder builder,
			Object value) {

		if (value == null) {
			return;
		}


		String text =
				String.valueOf(
						value
				);


		boolean needQuote =
				text.contains(",")
				|| text.contains("\"")
				|| text.contains("\n")
				|| text.contains("\r");


		if (!needQuote) {

			builder.append(
					text
			);

			return;
		}


		builder.append('"');


		builder.append(
				text.replace(
						"\"",
						"\"\""
				)
		);


		builder.append('"');
	}


	private static Object nullable(
			Object value) {

		return value == null
				? ""
				: value;
	}


	private static void writeUtf8BomFile(
			Path file,
			String text)
			throws Exception {

		String content =
				"\uFEFF"
				+ text;


		Files.writeString(
				file,
				content,
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING
		);
	}


	// =========================================================
	// 결과 요약
	// =========================================================

	private static void printSummary(
			List<ReviewRow> rows,
			List<UnresolvedFamilyRow> unresolvedFamilyRows) {

		Map<ReviewGroup, Integer> counts =
				new EnumMap<>(
						ReviewGroup.class
				);


		for (ReviewGroup group :
				ReviewGroup.values()) {

			counts.put(
					group,
					0
			);
		}


		for (ReviewRow row :
				rows) {

			counts.compute(
					row.group(),
					(key, value) ->
							value == null
									? 1
									: value + 1
			);
		}


		System.out.println();

		System.out.println(
				"total                       = "
				+ rows.size()
		);


		System.out.println(
				"material candidates         = "
				+ counts.get(
						ReviewGroup.MATERIAL_CANDIDATE
				)
		);


		System.out.println(
				"special entity candidates   = "
				+ counts.get(
						ReviewGroup.SPECIAL_ENTITY_CANDIDATE
				)
		);


		System.out.println(
				"playable candidates         = "
				+ counts.get(
						ReviewGroup.PLAYABLE_CANDIDATE
				)
		);


		System.out.println(
				"battle form candidates      = "
				+ counts.get(
						ReviewGroup.BATTLE_FORM_CANDIDATE
				)
		);


		System.out.println(
				"related form review         = "
				+ counts.get(
						ReviewGroup.RELATED_FORM_REVIEW
				)
		);


		System.out.println(
				"unresolved family review    = "
				+ counts.get(
						ReviewGroup.UNRESOLVED_FAMILY_REVIEW
				)
		);


		System.out.println(
				"unresolved families         = "
				+ unresolvedFamilyRows.size()
		);
	}


	// =========================================================
	// Candidate 읽기
	// =========================================================

	private static List<MonsterData> readMonsters(
			Path file)
			throws Exception {

		try (InputStream inputStream =
				Files.newInputStream(
						file
				)) {

			return JSON_MAPPER.readValue(
					inputStream,
					new TypeReference<
							List<MonsterData>>() {}
			);
		}
	}


	private static Map<Integer, MonsterLocalization>
			readLocalizations(
					Path file)
					throws Exception {

		try (InputStream inputStream =
				Files.newInputStream(
						file
				)) {

			return JSON_MAPPER.readValue(
					inputStream,
					new TypeReference<
							Map<Integer, MonsterLocalization>>() {}
			);
		}
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
	// 보고서 전용 타입
	// =========================================================

	private enum ReviewGroup {

		MATERIAL_CANDIDATE,

		SPECIAL_ENTITY_CANDIDATE,

		PLAYABLE_CANDIDATE,

		BATTLE_FORM_CANDIDATE,

		RELATED_FORM_REVIEW,

		UNRESOLVED_FAMILY_REVIEW
	}


	private record ReviewRow(

			ReviewGroup group,

			int internalId,

			Integer swarfarmId,

			Integer com2usId,

			String name,

			String element,

			String archetype,

			String awakeningStage,

			boolean obtainable,

			boolean homunculus,

			boolean fusionFood,

			Integer familyId,

			int familyMemberCount,

			int familyObtainableCount,

			boolean familyHasPlayableSignal,

			int skillCount,

			String missingStats,

			Integer transformsToId,

			String transformsToName,

			String transformedFromIds,

			String transformedFromNames,

			boolean transformLinked,

			String reasons) {
	}


	private record UnresolvedFamilyRow(

			int familyId,

			int unresolvedMemberCount,

			int totalFamilyMemberCount,

			int obtainableMemberCount,

			int playableCandidateCount,

			int battleFormCandidateCount,

			int relatedFormReviewCount,

			String members) {
	}
}