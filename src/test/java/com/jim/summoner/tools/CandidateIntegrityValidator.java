package com.jim.summoner.tools;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import com.jim.summoner.data.model.FamilyData;
import com.jim.summoner.data.model.LeaderSkillData;
import com.jim.summoner.data.model.MonsterData;
import com.jim.summoner.data.model.MonsterLocalization;
import com.jim.summoner.data.model.MonsterSkillRef;
import com.jim.summoner.data.model.MonsterSourceData;
import com.jim.summoner.data.model.MonsterSourceLocalization;
import com.jim.summoner.data.model.SkillData;
import com.jim.summoner.data.model.SkillEffectData;
import com.jim.summoner.data.model.SkillEffectLocalization;
import com.jim.summoner.data.model.SkillEffectRefData;
import com.jim.summoner.data.model.SkillLevelUpData;
import com.jim.summoner.data.model.SkillLocalization;
import com.jim.summoner.data.model.SourceIds;
import com.jim.summoner.data.type.MonsterEntityType;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

public class CandidateIntegrityValidator {

	private static final JsonMapper JSON_MAPPER =
			new JsonMapper();

	private static final int MAX_PRINTED_PROBLEMS =
			50;


	public static void main(String[] args)
			throws Exception {

		Path projectRoot =
				resolveProjectRoot();


		Path normalizedDirectory =
				projectRoot.resolve(
						Path.of(
								"data-source",
								"candidate",
								"normalized"
						)
				);


		Path localizationEnDirectory =
				projectRoot.resolve(
						Path.of(
								"data-source",
								"candidate",
								"localization",
								"en"
						)
				);


		System.out.println(
				"========================================"
		);

		System.out.println(
				"Candidate integrity validation start"
		);

		System.out.println(
				"========================================"
		);


		List<MonsterData> monsters =
				readJson(
						normalizedDirectory.resolve(
								"monsters.json"
						),
						new TypeReference<
								List<MonsterData>>() {}
				);


		List<FamilyData> families =
				readJson(
						normalizedDirectory.resolve(
								"families.json"
						),
						new TypeReference<
								List<FamilyData>>() {}
				);


		List<SkillData> skills =
				readJson(
						normalizedDirectory.resolve(
								"skills.json"
						),
						new TypeReference<
								List<SkillData>>() {}
				);


		List<SkillEffectData> skillEffects =
				readJson(
						normalizedDirectory.resolve(
								"skill-effects.json"
						),
						new TypeReference<
								List<SkillEffectData>>() {}
				);


		List<LeaderSkillData> leaderSkills =
				readJson(
						normalizedDirectory.resolve(
								"leader-skills.json"
						),
						new TypeReference<
								List<LeaderSkillData>>() {}
				);


		List<MonsterSourceData> monsterSources =
				readJson(
						normalizedDirectory.resolve(
								"monster-sources.json"
						),
						new TypeReference<
								List<MonsterSourceData>>() {}
				);


		Map<Integer, MonsterLocalization>
				monsterLocalizations =
						readJson(
								localizationEnDirectory.resolve(
										"monsters.json"
								),
								new TypeReference<
										Map<Integer, MonsterLocalization>>() {}
						);


		Map<Integer, SkillLocalization>
				skillLocalizations =
						readJson(
								localizationEnDirectory.resolve(
										"skills.json"
								),
								new TypeReference<
										Map<Integer, SkillLocalization>>() {}
						);


		Map<Integer, SkillEffectLocalization>
				skillEffectLocalizations =
						readJson(
								localizationEnDirectory.resolve(
										"skill-effects.json"
								),
								new TypeReference<
										Map<Integer, SkillEffectLocalization>>() {}
						);


		Map<Integer, MonsterSourceLocalization>
				monsterSourceLocalizations =
						readJson(
								localizationEnDirectory.resolve(
										"monster-sources.json"
								),
								new TypeReference<
										Map<Integer, MonsterSourceLocalization>>() {}
						);


		ValidationState state =
				new ValidationState();

		// KO is optional and partial; retain the existing full-coverage EN policy.
		Path localizationKoDirectory = projectRoot.resolve("data-source/candidate/localization/ko");
		for (String dataset : KoreanLocalizationValidation.DATASETS) {
			Path file = localizationKoDirectory.resolve(dataset + ".json");
			if (Files.exists(file)) {
				try {
					KoreanLocalizationValidation.validate(
							normalizedDirectory.resolve(dataset + ".json"), file, dataset);
				} catch (Exception failure) {
					state.errors.add("KO localization validation failed: " + file + ": " + failure.getMessage());
				}
			}
		}


		Map<Integer, MonsterData> monsterById =
				toIdMap(
						monsters,
						MonsterData::getId,
						"Monster",
						state
				);


		Map<Integer, FamilyData> familyById =
				toIdMap(
						families,
						FamilyData::getId,
						"Family",
						state
				);


		Map<Integer, SkillData> skillById =
				toIdMap(
						skills,
						SkillData::getId,
						"Skill",
						state
				);


		Map<Integer, SkillEffectData> skillEffectById =
				toIdMap(
						skillEffects,
						SkillEffectData::getId,
						"SkillEffect",
						state
				);


		Map<Integer, LeaderSkillData> leaderSkillById =
				toIdMap(
						leaderSkills,
						LeaderSkillData::getId,
						"LeaderSkill",
						state
				);


		Map<Integer, MonsterSourceData> monsterSourceById =
				toIdMap(
						monsterSources,
						MonsterSourceData::getId,
						"MonsterSource",
						state
				);


		validateLocalizationCoverage(
				"Monster",
				monsterById.keySet(),
				monsterLocalizations.keySet(),
				state
		);


		validateLocalizationCoverage(
				"Skill",
				skillById.keySet(),
				skillLocalizations.keySet(),
				state
		);


		validateLocalizationCoverage(
				"SkillEffect",
				skillEffectById.keySet(),
				skillEffectLocalizations.keySet(),
				state
		);


		validateLocalizationCoverage(
				"MonsterSource",
				monsterSourceById.keySet(),
				monsterSourceLocalizations.keySet(),
				state
		);


		validateFamilies(
				familyById,
				monsterById,
				state
		);


		validateMonsters(
				monsterById,
				familyById,
				skillById,
				leaderSkillById,
				monsterSourceById,
				state
		);


		validateSkills(
				skillById,
				skillEffectById,
				skillLocalizations,
				state
		);


		validateSkillEffects(
				skillEffectById,
				skillEffectLocalizations,
				state
		);


		validateLeaderSkills(
				leaderSkillById,
				state
		);


		validateMonsterSources(
				monsterSourceById,
				monsterSourceLocalizations,
				state
		);


		validateMonsterLocalizations(
				monsterLocalizations,
				state
		);


		printSummary(
				monsterById,
				familyById,
				skillById,
				skillEffectById,
				leaderSkillById,
				monsterSourceById,
				monsterLocalizations,
				skillLocalizations,
				skillEffectLocalizations,
				monsterSourceLocalizations,
				state
		);


		if (!state.errors.isEmpty()) {

			System.out.println();

			System.out.println(
					"[ERRORS] "
					+ state.errors.size()
			);


			printProblems(
					state.errors
			);


			System.out.println();

			System.out.println(
					"========================================"
			);

			System.out.println(
					"Candidate integrity validation FAILED"
			);

			System.out.println(
					"========================================"
			);


			throw new IllegalStateException(
					"Candidate integrity validation failed."
					+ " errors="
					+ state.errors.size()
			);
		}


		if (!state.warnings.isEmpty()) {

			System.out.println();

			System.out.println(
					"[WARNINGS] "
					+ state.warnings.size()
			);


			printProblems(
					state.warnings
			);
		}


		System.out.println();

		System.out.println(
				"========================================"
		);

		System.out.println(
				"Candidate integrity validation PASSED"
		);

		System.out.println(
				"========================================"
		);
	}


	// =========================================================
	// Family
	// =========================================================

	private static void validateFamilies(
			Map<Integer, FamilyData> families,
			Map<Integer, MonsterData> monsters,
			ValidationState state) {

		for (FamilyData family :
				families.values()) {

			validateEntityBasics(
					"Family",
					family.getId(),
					family.getSourceIds(),
					state
			);


			if (family.getMonsterIds()
					== null) {

				state.error(
						"Family monsterIds is null."
						+ " familyId="
						+ family.getId()
				);

				continue;
			}


			Set<Integer> seen =
					new HashSet<>();


			for (Integer monsterId :
					family.getMonsterIds()) {

				if (monsterId == null) {

					state.error(
							"Family contains null monsterId."
							+ " familyId="
							+ family.getId()
					);

					continue;
				}


				if (!seen.add(
						monsterId
				)) {

					state.error(
							"Family contains duplicate monsterId."
							+ " familyId="
							+ family.getId()
							+ ", monsterId="
							+ monsterId
					);
				}


				MonsterData monster =
						monsters.get(
								monsterId
						);


				if (monster == null) {

					state.error(
							"Family references missing Monster."
							+ " familyId="
							+ family.getId()
							+ ", monsterId="
							+ monsterId
					);

					continue;
				}


				if (!Integer.valueOf(
						family.getId()
				)
				.equals(
						monster.getFamilyId()
				)) {

					state.error(
							"Family/Monster familyId mismatch."
							+ " familyId="
							+ family.getId()
							+ ", monsterId="
							+ monsterId
							+ ", monster.familyId="
							+ monster.getFamilyId()
					);
				}
			}
		}
	}


	// =========================================================
	// Monster
	// =========================================================

	private static void validateMonsters(
			Map<Integer, MonsterData> monsters,
			Map<Integer, FamilyData> families,
			Map<Integer, SkillData> skills,
			Map<Integer, LeaderSkillData> leaderSkills,
			Map<Integer, MonsterSourceData> monsterSources,
			ValidationState state) {

		for (MonsterData monster :
				monsters.values()) {

			int monsterId =
					monster.getId();


			validateEntityBasics(
					"Monster",
					monsterId,
					monster.getSourceIds(),
					state
			);


			if (monster.getElement()
					== null) {

				state.error(
						"Monster element is null."
						+ " monsterId="
						+ monsterId
				);
			}


			if (monster.getArchetype()
					== null) {

				state.error(
						"Monster archetype is null."
						+ " monsterId="
						+ monsterId
				);
			}


			if (monster.getEntityType()
					== null) {

				state.error(
						"Monster entityType is null."
						+ " monsterId="
						+ monsterId
				);
			}
			else if (monster.getEntityType()
					== MonsterEntityType.UNKNOWN) {

				state.error(
						"Monster remains UNKNOWN."
						+ " monsterId="
						+ monsterId
				);
			}


			if (monster.getAwakening()
					== null
					|| monster.getAwakening()
							.getStage()
							== null) {

				state.error(
						"Monster awakening/stage is null."
						+ " monsterId="
						+ monsterId
				);
			}


			if (monster.getBaseStats()
					== null) {

				state.error(
						"Monster baseStats is null."
						+ " monsterId="
						+ monsterId
				);
			}


			if (monster.getStats()
					== null) {

				state.error(
						"Monster stats is null."
						+ " monsterId="
						+ monsterId
				);
			}


			if (monster.getFlags()
					== null) {

				state.error(
						"Monster flags is null."
						+ " monsterId="
						+ monsterId
				);
			}


			if (monster.getAssets()
					== null
					|| isBlank(
							monster.getAssets()
									.getIconKey()
					)) {

				state.error(
						"Monster asset/iconKey is missing."
						+ " monsterId="
						+ monsterId
				);
			}


			validateMonsterFamily(
					monster,
					families,
					state
			);


			validateAwakening(
					monster,
					monsters,
					state
			);


			validateMonsterSkills(
					monster,
					skills,
					state
			);


			Integer leaderSkillId =
					monster.getLeaderSkillId();


			if (leaderSkillId != null) {

				state.leaderSkillReferences++;


				if (!leaderSkills.containsKey(
						leaderSkillId
				)) {

					state.error(
							"Monster references missing LeaderSkill."
							+ " monsterId="
							+ monsterId
							+ ", leaderSkillId="
							+ leaderSkillId
					);
				}
			}


			if (monster.getObtainSourceIds()
					== null) {

				state.error(
						"Monster obtainSourceIds is null."
						+ " monsterId="
						+ monsterId
				);
			}
			else {

				for (Integer sourceId :
						monster.getObtainSourceIds()) {

					state.monsterSourceReferences++;


					if (sourceId == null
							|| !monsterSources
									.containsKey(
											sourceId
									)) {

						state.error(
								"Monster references missing MonsterSource."
								+ " monsterId="
								+ monsterId
								+ ", sourceId="
								+ sourceId
						);
					}
				}
			}


			Integer transformsToId =
					monster.getTransformsToId();


			if (transformsToId != null) {

				state.transformReferences++;


				if (!monsters.containsKey(
						transformsToId
				)) {

					state.error(
							"Monster references missing transformsToId."
							+ " monsterId="
							+ monsterId
							+ ", transformsToId="
							+ transformsToId
					);
				}
			}


			validatePlayableStats(
					monster,
					state
			);
		}
	}


	private static void validateMonsterFamily(
			MonsterData monster,
			Map<Integer, FamilyData> families,
			ValidationState state) {

		Integer familyId =
				monster.getFamilyId();


		if (familyId == null) {
			return;
		}


		state.familyReferences++;


		FamilyData family =
				families.get(
						familyId
				);


		if (family == null) {

			state.error(
					"Monster references missing Family."
					+ " monsterId="
					+ monster.getId()
					+ ", familyId="
					+ familyId
			);

			return;
		}


		if (family.getMonsterIds()
				== null
				|| !family.getMonsterIds()
						.contains(
								monster.getId()
						)) {

			state.error(
					"Monster is not contained by its Family."
					+ " monsterId="
					+ monster.getId()
					+ ", familyId="
					+ familyId
			);
		}
	}


	private static void validateAwakening(
			MonsterData monster,
			Map<Integer, MonsterData> monsters,
			ValidationState state) {

		if (monster.getAwakening()
				== null) {
			return;
		}


		Integer previousId =
				monster.getAwakening()
						.getPreviousFormId();


		if (previousId != null) {

			state.awakeningReferences++;


			MonsterData previous =
					monsters.get(
							previousId
					);


			if (previous == null) {

				state.error(
						"Monster references missing previousFormId."
						+ " monsterId="
						+ monster.getId()
						+ ", previousFormId="
						+ previousId
				);
			}
			else if (previous.getAwakening()
					== null
					|| !Integer.valueOf(
							monster.getId()
					)
					.equals(
							previous.getAwakening()
									.getNextFormId()
					)) {

				state.warning(
						"Awakening previous relation is not reciprocal."
						+ " monsterId="
						+ monster.getId()
						+ ", previousFormId="
						+ previousId
				);
			}
		}


		Integer nextId =
				monster.getAwakening()
						.getNextFormId();


		if (nextId != null) {

			state.awakeningReferences++;


			MonsterData next =
					monsters.get(
							nextId
					);


			if (next == null) {

				state.error(
						"Monster references missing nextFormId."
						+ " monsterId="
						+ monster.getId()
						+ ", nextFormId="
						+ nextId
				);
			}
			else if (next.getAwakening()
					== null
					|| !Integer.valueOf(
							monster.getId()
					)
					.equals(
							next.getAwakening()
									.getPreviousFormId()
					)) {

				state.warning(
						"Awakening next relation is not reciprocal."
						+ " monsterId="
						+ monster.getId()
						+ ", nextFormId="
						+ nextId
				);
			}
		}
	}


	private static void validateMonsterSkills(
			MonsterData monster,
			Map<Integer, SkillData> skills,
			ValidationState state) {

		if (monster.getSkills()
				== null) {

			state.error(
					"Monster skills is null."
					+ " monsterId="
					+ monster.getId()
			);

			return;
		}


		Set<Integer> seenSlots =
				new HashSet<>();


		for (MonsterSkillRef skillRef :
				monster.getSkills()) {

			state.monsterSkillReferences++;


			if (skillRef == null) {

				state.error(
						"Monster contains null Skill reference."
						+ " monsterId="
						+ monster.getId()
				);

				continue;
			}


			SkillData skill =
					skills.get(
							skillRef.getSkillId()
					);


			if (skill == null) {

				state.error(
						"Monster references missing Skill."
						+ " monsterId="
						+ monster.getId()
						+ ", skillId="
						+ skillRef.getSkillId()
				);
			}
			else if (skillRef.getSlot()
					!= skill.getSlot()) {

				state.error(
						"Monster Skill slot differs from Skill slot."
						+ " monsterId="
						+ monster.getId()
						+ ", skillId="
						+ skillRef.getSkillId()
						+ ", monsterSlot="
						+ skillRef.getSlot()
						+ ", skillSlot="
						+ skill.getSlot()
				);
			}


			if (!seenSlots.add(
					skillRef.getSlot()
			)) {

				state.warning(
						"Monster has duplicate Skill slot."
						+ " monsterId="
						+ monster.getId()
						+ ", slot="
						+ skillRef.getSlot()
				);
			}
		}
	}


	private static void validatePlayableStats(
			MonsterData monster,
			ValidationState state) {

		if (monster.getEntityType()
				!= MonsterEntityType.PLAYABLE) {

			return;
		}


		boolean missing =
				monster.getBaseStats()
						== null
				|| monster.getBaseStats()
						.getHp()
						== null
				|| monster.getBaseStats()
						.getAttack()
						== null
				|| monster.getBaseStats()
						.getDefense()
						== null
				|| monster.getStats()
						== null
				|| monster.getStats()
						.getHp()
						== null
				|| monster.getStats()
						.getAttack()
						== null
				|| monster.getStats()
						.getDefense()
						== null
				|| monster.getStats()
						.getSpeed()
						== null
				|| monster.getStats()
						.getCritRate()
						== null
				|| monster.getStats()
						.getCritDamage()
						== null
				|| monster.getStats()
						.getResistance()
						== null
				|| monster.getStats()
						.getAccuracy()
						== null;


		if (missing) {

			state.error(
					"PLAYABLE Monster has missing stat value."
					+ " monsterId="
					+ monster.getId()
			);
		}
	}


	// =========================================================
	// Skill
	// =========================================================

	private static void validateSkills(
			Map<Integer, SkillData> skills,
			Map<Integer, SkillEffectData> skillEffects,
			Map<Integer, SkillLocalization> localizations,
			ValidationState state) {

		for (SkillData skill :
				skills.values()) {

			int skillId =
					skill.getId();


			validateEntityBasics(
					"Skill",
					skillId,
					skill.getSourceIds(),
					state
			);


			if (skill.getFlags()
					== null) {

				state.error(
						"Skill flags is null."
						+ " skillId="
						+ skillId
				);
			}


			if (skill.getScaling()
					== null) {

				state.error(
						"Skill scaling is null."
						+ " skillId="
						+ skillId
				);
			}
			else {

				if (skill.getScaling()
						.getExpression()
						== null) {

					state.error(
							"Skill scaling.expression is null."
							+ " skillId="
							+ skillId
					);
				}


				if (skill.getScaling()
						.getStats()
						== null) {

					state.error(
							"Skill scaling.stats is null."
							+ " skillId="
							+ skillId
					);
				}
			}


			if (skill.getAssets()
					== null
					|| isBlank(
							skill.getAssets()
									.getIconKey()
					)) {

				state.error(
						"Skill asset/iconKey is missing."
						+ " skillId="
						+ skillId
				);
			}


			validateSkillEffects(
					skill,
					skillEffects,
					state
			);


			validateSkillLevelUps(
					skill,
					localizations.get(
							skillId
					),
					state
			);


			Integer relatedSkillId =
					skill.getRelatedSkillId();


			if (relatedSkillId != null) {

				state.relatedSkillReferences++;


				if (!skills.containsKey(
						relatedSkillId
				)) {

					state.error(
							"Skill references missing relatedSkillId."
							+ " skillId="
							+ skillId
							+ ", relatedSkillId="
							+ relatedSkillId
					);
				}
			}


			SkillLocalization localization =
					localizations.get(
							skillId
					);


			if (localization != null) {

				if (isBlank(
						localization.getName()
				)) {

					state.error(
							"Skill localization name is blank."
							+ " skillId="
							+ skillId
					);
				}


				if (localization.getDescription()
						== null) {

					state.error(
							"Skill localization description is null."
							+ " skillId="
							+ skillId
					);
				}
			}
		}
	}


	private static void validateSkillEffects(
			SkillData skill,
			Map<Integer, SkillEffectData> skillEffects,
			ValidationState state) {

		if (skill.getEffects()
				== null) {

			state.error(
					"Skill effects is null."
					+ " skillId="
					+ skill.getId()
			);

			return;
		}


		for (SkillEffectRefData reference :
				skill.getEffects()) {

			state.skillEffectReferences++;


			if (reference == null) {

				state.error(
						"Skill contains null effect reference."
						+ " skillId="
						+ skill.getId()
				);

				continue;
			}


			if (!skillEffects.containsKey(
					reference.getEffectId()
			)) {

				state.error(
						"Skill references missing SkillEffect."
						+ " skillId="
						+ skill.getId()
						+ ", effectId="
						+ reference.getEffectId()
				);
			}


			if (reference.getFlags()
					== null) {

				state.error(
						"Skill effect reference flags is null."
						+ " skillId="
						+ skill.getId()
						+ ", effectId="
						+ reference.getEffectId()
				);
			}
		}
	}


	private static void validateSkillLevelUps(
			SkillData skill,
			SkillLocalization localization,
			ValidationState state) {

		if (skill.getLevelUps()
				== null) {

			state.error(
					"Skill levelUps is null."
					+ " skillId="
					+ skill.getId()
			);

			return;
		}


		for (int index = 0;
				index < skill.getLevelUps()
						.size();
				index++) {

			SkillLevelUpData levelUp =
					skill.getLevelUps()
							.get(
									index
							);


			if (levelUp == null) {

				state.error(
						"Skill contains null levelUp."
						+ " skillId="
						+ skill.getId()
						+ ", index="
						+ index
				);

				continue;
			}


			int expectedLevel =
					index + 2;


			if (levelUp.getLevel()
					!= expectedLevel) {

				state.error(
						"Skill levelUp sequence mismatch."
						+ " skillId="
						+ skill.getId()
						+ ", expectedLevel="
						+ expectedLevel
						+ ", actualLevel="
						+ levelUp.getLevel()
				);
			}


			if (isBlank(
					levelUp.getEffectTemplate()
			)) {

				state.error(
						"Skill levelUp effectTemplate is blank."
						+ " skillId="
						+ skill.getId()
						+ ", level="
						+ levelUp.getLevel()
				);
			}
		}


		if (skill.getMaxLevel()
				!= skill.getLevelUps()
						.size()
						+ 1) {

			state.maxLevelMismatchCount++;


			state.warning(
					"Skill maxLevel differs from levelUps size + 1."
					+ " skillId="
					+ skill.getId()
					+ ", maxLevel="
					+ skill.getMaxLevel()
					+ ", levelUps="
					+ skill.getLevelUps()
							.size()
			);
		}


		if (localization == null) {
			return;
		}


		if (localization.getLevelUpDescriptions()
				== null) {

			state.error(
					"Skill localization levelUpDescriptions is null."
					+ " skillId="
					+ skill.getId()
			);

			return;
		}


		if (localization.getLevelUpDescriptions()
				.size()
				!= skill.getLevelUps()
						.size()) {

			state.error(
					"Skill levelUp localization size mismatch."
					+ " skillId="
					+ skill.getId()
					+ ", levelUps="
					+ skill.getLevelUps()
							.size()
					+ ", descriptions="
					+ localization.getLevelUpDescriptions()
							.size()
			);
		}
	}


	// =========================================================
	// Skill Effect Entity
	// =========================================================

	private static void validateSkillEffects(
			Map<Integer, SkillEffectData> skillEffects,
			Map<Integer, SkillEffectLocalization> localizations,
			ValidationState state) {

		for (SkillEffectData effect :
				skillEffects.values()) {

			int effectId =
					effect.getId();


			validateEntityBasics(
					"SkillEffect",
					effectId,
					effect.getSourceIds(),
					state
			);


			if (effect.getType()
					== null) {

				state.error(
						"SkillEffect type is null."
						+ " effectId="
						+ effectId
				);
			}


			if (effect.getFlags()
					== null) {

				state.error(
						"SkillEffect flags is null."
						+ " effectId="
						+ effectId
				);
			}


			SkillEffectLocalization localization =
					localizations.get(
							effectId
					);


			if (localization == null) {
				continue;
			}


			if (isBlank(
					localization.getName()
			)) {

				state.error(
						"SkillEffect localization name is blank."
						+ " effectId="
						+ effectId
				);
			}


			if (isBlank(
					localization.getDescription()
			)) {

				state.error(
						"SkillEffect localization description is blank."
						+ " effectId="
						+ effectId
				);
			}
		}
	}


	// =========================================================
	// Leader Skill
	// =========================================================

	private static void validateLeaderSkills(
			Map<Integer, LeaderSkillData> leaderSkills,
			ValidationState state) {

		Set<String> validStats =
				Set.of(
						"ACCURACY",
						"ATTACK",
						"SPEED",
						"CRIT_RATE",
						"CRIT_DAMAGE",
						"DEFENSE",
						"HP",
						"RESISTANCE"
				);


		Set<String> validAreas =
				Set.of(
						"GENERAL",
						"ARENA",
						"GUILD",
						"DUNGEON",
						"ELEMENT"
				);


		Set<String> validElements =
				Set.of(
						"FIRE",
						"WATER",
						"WIND",
						"LIGHT",
						"DARK"
				);


		for (LeaderSkillData leaderSkill :
				leaderSkills.values()) {

			int leaderSkillId =
					leaderSkill.getId();


			validateEntityBasics(
					"LeaderSkill",
					leaderSkillId,
					leaderSkill.getSourceIds(),
					state
			);


			if (!validStats.contains(
					leaderSkill.getStat()
			)) {

				state.error(
						"LeaderSkill has invalid stat."
						+ " leaderSkillId="
						+ leaderSkillId
						+ ", stat="
						+ leaderSkill.getStat()
				);
			}


			if (!validAreas.contains(
					leaderSkill.getArea()
			)) {

				state.error(
						"LeaderSkill has invalid area."
						+ " leaderSkillId="
						+ leaderSkillId
						+ ", area="
						+ leaderSkill.getArea()
				);
			}


			if (leaderSkill.getElement()
					!= null
					&& !validElements.contains(
							leaderSkill.getElement()
					)) {

				state.error(
						"LeaderSkill has invalid element."
						+ " leaderSkillId="
						+ leaderSkillId
						+ ", element="
						+ leaderSkill.getElement()
				);
			}
		}
	}


	// =========================================================
	// Monster Source
	// =========================================================

	private static void validateMonsterSources(
			Map<Integer, MonsterSourceData> sources,
			Map<Integer, MonsterSourceLocalization> localizations,
			ValidationState state) {

		for (MonsterSourceData source :
				sources.values()) {

			int sourceId =
					source.getId();


			validateEntityBasics(
					"MonsterSource",
					sourceId,
					source.getSourceIds(),
					state
			);


			MonsterSourceLocalization localization =
					localizations.get(
							sourceId
					);


			if (localization == null) {
				continue;
			}


			if (isBlank(
					localization.getName()
			)) {

				state.error(
						"MonsterSource localization name is blank."
						+ " sourceId="
						+ sourceId
				);
			}


			if (localization.getDescription()
					== null) {

				state.error(
						"MonsterSource localization description is null."
						+ " sourceId="
						+ sourceId
				);
			}
		}
	}


	// =========================================================
	// Monster Localization
	// =========================================================

	private static void validateMonsterLocalizations(
			Map<Integer, MonsterLocalization> localizations,
			ValidationState state) {

		for (Map.Entry<Integer, MonsterLocalization> entry :
				localizations.entrySet()) {

			if (entry.getValue()
					== null
					|| isBlank(
							entry.getValue()
									.getName()
					)) {

				state.error(
						"Monster localization name is missing."
						+ " monsterId="
						+ entry.getKey()
				);
			}
		}
	}


	// =========================================================
	// 공통 Entity 검사
	// =========================================================

	private static void validateEntityBasics(
			String type,
			int id,
			SourceIds sourceIds,
			ValidationState state) {

		if (id <= 0) {

			state.error(
					type
					+ " internal id must be positive."
					+ " id="
					+ id
			);
		}


		if (sourceIds == null
				|| sourceIds.getSwarfarm()
						== null
				|| sourceIds.getSwarfarm()
						<= 0) {

			state.error(
					type
					+ " sourceIds.swarfarm is missing/invalid."
					+ " id="
					+ id
			);
		}
	}


	private static <T> Map<Integer, T> toIdMap(
			List<T> list,
			Function<T, Integer> idExtractor,
			String type,
			ValidationState state) {

		Map<Integer, T> result =
				new LinkedHashMap<>();


		if (list == null) {

			state.error(
					type
					+ " list is null."
			);

			return result;
		}


		for (T item :
				list) {

			if (item == null) {

				state.error(
						type
						+ " list contains null item."
				);

				continue;
			}


			Integer id =
					idExtractor.apply(
							item
					);


			if (result.put(
					id,
					item
			) != null) {

				state.error(
						"Duplicate "
						+ type
						+ " internal id."
						+ " id="
						+ id
				);
			}
		}


		return result;
	}


	// =========================================================
	// Localization Coverage
	// =========================================================

	private static void validateLocalizationCoverage(
			String type,
			Set<Integer> entityIds,
			Set<Integer> localizationIds,
			ValidationState state) {

		for (Integer id :
				entityIds) {

			if (!localizationIds.contains(
					id
			)) {

				state.error(
						type
						+ " EN localization missing."
						+ " id="
						+ id
				);
			}
		}


		for (Integer id :
				localizationIds) {

			if (!entityIds.contains(
					id
			)) {

				state.error(
						type
						+ " EN localization is orphaned."
						+ " id="
						+ id
				);
			}
		}
	}


	// =========================================================
	// 결과 출력
	// =========================================================

	private static void printSummary(
			Map<Integer, MonsterData> monsters,
			Map<Integer, FamilyData> families,
			Map<Integer, SkillData> skills,
			Map<Integer, SkillEffectData> skillEffects,
			Map<Integer, LeaderSkillData> leaderSkills,
			Map<Integer, MonsterSourceData> monsterSources,
			Map<Integer, MonsterLocalization> monsterLocalizations,
			Map<Integer, SkillLocalization> skillLocalizations,
			Map<Integer, SkillEffectLocalization> skillEffectLocalizations,
			Map<Integer, MonsterSourceLocalization> monsterSourceLocalizations,
			ValidationState state) {

		System.out.println();

		System.out.println(
				"[ENTITY COUNTS]"
		);

		System.out.println(
				"monsters        = "
				+ monsters.size()
		);

		System.out.println(
				"families        = "
				+ families.size()
		);

		System.out.println(
				"skills          = "
				+ skills.size()
		);

		System.out.println(
				"skill effects   = "
				+ skillEffects.size()
		);

		System.out.println(
				"leader skills   = "
				+ leaderSkills.size()
		);

		System.out.println(
				"monster sources = "
				+ monsterSources.size()
		);


		System.out.println();

		System.out.println(
				"[EN LOCALIZATION COUNTS]"
		);

		System.out.println(
				"monsters        = "
				+ monsterLocalizations.size()
		);

		System.out.println(
				"skills          = "
				+ skillLocalizations.size()
		);

		System.out.println(
				"skill effects   = "
				+ skillEffectLocalizations.size()
		);

		System.out.println(
				"monster sources = "
				+ monsterSourceLocalizations.size()
		);


		System.out.println();

		System.out.println(
				"[REFERENCE COUNTS]"
		);

		System.out.println(
				"family links          = "
				+ state.familyReferences
		);

		System.out.println(
				"monster -> skill      = "
				+ state.monsterSkillReferences
		);

		System.out.println(
				"monster -> leader     = "
				+ state.leaderSkillReferences
		);

		System.out.println(
				"monster -> source     = "
				+ state.monsterSourceReferences
		);

		System.out.println(
				"awakening references  = "
				+ state.awakeningReferences
		);

		System.out.println(
				"transform references  = "
				+ state.transformReferences
		);

		System.out.println(
				"skill -> effect       = "
				+ state.skillEffectReferences
		);

		System.out.println(
				"skill -> relatedSkill = "
				+ state.relatedSkillReferences
		);

		System.out.println(
				"maxLevel mismatches   = "
				+ state.maxLevelMismatchCount
				+ " (warning only)"
		);


		Map<MonsterEntityType, Integer> entityTypeCounts =
				new TreeMap<>(
						(left, right) ->
								left.name()
										.compareTo(
												right.name()
										)
				);


		for (MonsterData monster :
				monsters.values()) {

			if (monster.getEntityType()
					!= null) {

				entityTypeCounts.merge(
						monster.getEntityType(),
						1,
						Integer::sum
				);
			}
		}


		System.out.println();

		System.out.println(
				"[MONSTER ENTITY TYPES]"
		);


		for (Map.Entry<MonsterEntityType, Integer> entry :
				entityTypeCounts.entrySet()) {

			System.out.println(
					entry.getKey()
					+ " = "
					+ entry.getValue()
			);
		}
	}


	private static void printProblems(
			List<String> problems) {

		int limit =
				Math.min(
						MAX_PRINTED_PROBLEMS,
						problems.size()
				);


		for (int index = 0;
				index < limit;
				index++) {

			System.out.println(
					"- "
					+ problems.get(
							index
					)
			);
		}


		if (problems.size()
				> limit) {

			System.out.println(
					"... "
					+ (
							problems.size()
							- limit
					)
					+ " more"
			);
		}
	}


	// =========================================================
	// JSON
	// =========================================================

	private static <T> T readJson(
			Path file,
			TypeReference<T> typeReference)
			throws Exception {

		if (!Files.exists(
				file
		)) {

			throw new IllegalStateException(
					"Required candidate file is missing."
					+ " file="
					+ file.toAbsolutePath()
			);
		}


		try (InputStream inputStream =
				Files.newInputStream(
						file
				)) {

			return JSON_MAPPER.readValue(
					inputStream,
					typeReference
			);
		}
	}


	private static boolean isBlank(
			String value) {

		return value == null
				|| value.isBlank();
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
				"Project root not found."
				+ " user.dir="
				+ current
		);
	}


	// =========================================================
	// 검증 상태
	// =========================================================

	private static class ValidationState {

		private final List<String> errors =
				new ArrayList<>();

		private final List<String> warnings =
				new ArrayList<>();


		private int familyReferences;

		private int monsterSkillReferences;

		private int leaderSkillReferences;

		private int monsterSourceReferences;

		private int awakeningReferences;

		private int transformReferences;

		private int skillEffectReferences;

		private int relatedSkillReferences;

		private int maxLevelMismatchCount;


		private void error(
				String message) {

			errors.add(
					message
			);
		}


		private void warning(
				String message) {

			warnings.add(
					message
			);
		}
	}
}
