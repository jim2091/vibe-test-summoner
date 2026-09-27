package com.jim.summoner.data;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

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

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class GameDataStore {

	private final JsonMapper jsonMapper;

	// =========================================================
	// Monster / Family
	// =========================================================

	private Map<Integer, MonsterData> monsters =
			Collections.emptyMap();

	private Map<Integer, FamilyData> families =
			Collections.emptyMap();

	private Map<Integer, MonsterLocalization> monsterLocalizationsEn =
			Collections.emptyMap();

	private Map<Integer, MonsterLocalization> monsterLocalizationsKo =
			Collections.emptyMap();

	// =========================================================
	// Skill
	// =========================================================

	private Map<Integer, SkillData> skills =
			Collections.emptyMap();

	private Map<Integer, SkillLocalization> skillLocalizationsEn =
			Collections.emptyMap();

	private Map<Integer, SkillLocalization> skillLocalizationsKo =
			Collections.emptyMap();

	// =========================================================
	// Skill Effect
	// =========================================================

	private Map<Integer, SkillEffectData> skillEffects =
			Collections.emptyMap();

	private Map<Integer, SkillEffectLocalization> skillEffectLocalizationsEn =
			Collections.emptyMap();

	private Map<Integer, SkillEffectLocalization> skillEffectLocalizationsKo =
			Collections.emptyMap();

	// =========================================================
	// Leader Skill
	// =========================================================

	private Map<Integer, LeaderSkillData> leaderSkills =
			Collections.emptyMap();

	// =========================================================
	// Monster Source
	// =========================================================

	private Map<Integer, MonsterSourceData> monsterSources =
			Collections.emptyMap();

	private Map<Integer, MonsterSourceLocalization> monsterSourceLocalizationsEn =
			Collections.emptyMap();

	private Map<Integer, MonsterSourceLocalization> monsterSourceLocalizationsKo =
			Collections.emptyMap();


	// =========================================================
	// 초기 로딩
	// =========================================================

	@PostConstruct
	public void load() {

		loadMonsters();
		loadFamilies();
		loadMonsterLocalizations();

		loadSkills();
		loadSkillLocalizations();

		loadSkillEffects();
		loadSkillEffectLocalizations();

		loadLeaderSkills();

		loadMonsterSources();
		loadMonsterSourceLocalizations();

		validateData();

		System.out.println(
				"[GameDataStore] data validation passed"
		);
	}


	// =========================================================
	// Monster / Family 로딩
	// =========================================================

	private void loadMonsters() {

		List<MonsterData> monsterList =
				readJson(
						"game-data/normalized/monsters.json",
						new TypeReference<
								List<MonsterData>>() {},
						"몬스터 데이터를 로딩할 수 없습니다."
				);

		monsters =
				toIdMap(
						monsterList,
						MonsterData::getId
				);

		System.out.println(
				"[GameDataStore] monsters loaded = "
				+ monsters.size()
		);
	}


	private void loadFamilies() {

		List<FamilyData> familyList =
				readJson(
						"game-data/normalized/families.json",
						new TypeReference<
								List<FamilyData>>() {},
						"몬스터 Family 데이터를 로딩할 수 없습니다."
				);

		families =
				toIdMap(
						familyList,
						FamilyData::getId
				);

		System.out.println(
				"[GameDataStore] families loaded = "
				+ families.size()
		);
	}


	private void loadMonsterLocalizations() {

		monsterLocalizationsEn =
				readJson(
						"game-data/localization/en/monsters.json",
						new TypeReference<
								Map<Integer, MonsterLocalization>>() {},
						"영문 몬스터 localization 데이터를 로딩할 수 없습니다."
				);

		monsterLocalizationsKo =
				readJson(
						"game-data/localization/ko/monsters.json",
						new TypeReference<
								Map<Integer, MonsterLocalization>>() {},
						"한글 몬스터 localization 데이터를 로딩할 수 없습니다."
				);

		monsterLocalizationsEn =
				Collections.unmodifiableMap(
						monsterLocalizationsEn
				);

		monsterLocalizationsKo =
				Collections.unmodifiableMap(
						monsterLocalizationsKo
				);

		System.out.println(
				"[GameDataStore] monster localization EN loaded = "
				+ monsterLocalizationsEn.size()
		);

		System.out.println(
				"[GameDataStore] monster localization KO loaded = "
				+ monsterLocalizationsKo.size()
		);
	}


	// =========================================================
	// Skill 로딩
	// =========================================================

	private void loadSkills() {

		List<SkillData> skillList =
				readJson(
						"game-data/normalized/skills.json",
						new TypeReference<
								List<SkillData>>() {},
						"스킬 데이터를 로딩할 수 없습니다."
				);

		skills =
				toIdMap(
						skillList,
						SkillData::getId
				);

		System.out.println(
				"[GameDataStore] skills loaded = "
				+ skills.size()
		);
	}


	private void loadSkillLocalizations() {

		skillLocalizationsEn =
				readJson(
						"game-data/localization/en/skills.json",
						new TypeReference<
								Map<Integer, SkillLocalization>>() {},
						"영문 스킬 localization 데이터를 로딩할 수 없습니다."
				);

		skillLocalizationsKo =
				readJson(
						"game-data/localization/ko/skills.json",
						new TypeReference<
								Map<Integer, SkillLocalization>>() {},
						"한글 스킬 localization 데이터를 로딩할 수 없습니다."
				);

		skillLocalizationsEn =
				Collections.unmodifiableMap(
						skillLocalizationsEn
				);

		skillLocalizationsKo =
				Collections.unmodifiableMap(
						skillLocalizationsKo
				);

		System.out.println(
				"[GameDataStore] skill localization EN loaded = "
				+ skillLocalizationsEn.size()
		);

		System.out.println(
				"[GameDataStore] skill localization KO loaded = "
				+ skillLocalizationsKo.size()
		);
	}


	// =========================================================
	// Skill Effect 로딩
	// =========================================================

	private void loadSkillEffects() {

		List<SkillEffectData> effectList =
				readJson(
						"game-data/normalized/skill-effects.json",
						new TypeReference<
								List<SkillEffectData>>() {},
						"스킬 효과 데이터를 로딩할 수 없습니다."
				);

		skillEffects =
				toIdMap(
						effectList,
						SkillEffectData::getId
				);

		System.out.println(
				"[GameDataStore] skill effects loaded = "
				+ skillEffects.size()
		);
	}


	private void loadSkillEffectLocalizations() {

		skillEffectLocalizationsEn =
				readJson(
						"game-data/localization/en/skill-effects.json",
						new TypeReference<
								Map<Integer, SkillEffectLocalization>>() {},
						"영문 스킬 효과 localization 데이터를 로딩할 수 없습니다."
				);

		skillEffectLocalizationsKo =
				readJson(
						"game-data/localization/ko/skill-effects.json",
						new TypeReference<
								Map<Integer, SkillEffectLocalization>>() {},
						"한글 스킬 효과 localization 데이터를 로딩할 수 없습니다."
				);

		skillEffectLocalizationsEn =
				Collections.unmodifiableMap(
						skillEffectLocalizationsEn
				);

		skillEffectLocalizationsKo =
				Collections.unmodifiableMap(
						skillEffectLocalizationsKo
				);

		System.out.println(
				"[GameDataStore] skill effect localization EN loaded = "
				+ skillEffectLocalizationsEn.size()
		);

		System.out.println(
				"[GameDataStore] skill effect localization KO loaded = "
				+ skillEffectLocalizationsKo.size()
		);
	}


	// =========================================================
	// Leader Skill 로딩
	// =========================================================

	private void loadLeaderSkills() {

		List<LeaderSkillData> leaderSkillList =
				readJson(
						"game-data/normalized/leader-skills.json",
						new TypeReference<
								List<LeaderSkillData>>() {},
						"리더 스킬 데이터를 로딩할 수 없습니다."
				);

		leaderSkills =
				toIdMap(
						leaderSkillList,
						LeaderSkillData::getId
				);

		System.out.println(
				"[GameDataStore] leader skills loaded = "
				+ leaderSkills.size()
		);
	}


	// =========================================================
	// Monster Source 로딩
	// =========================================================

	private void loadMonsterSources() {

		List<MonsterSourceData> sourceList =
				readJson(
						"game-data/normalized/monster-sources.json",
						new TypeReference<
								List<MonsterSourceData>>() {},
						"몬스터 획득처 데이터를 로딩할 수 없습니다."
				);

		monsterSources =
				toIdMap(
						sourceList,
						MonsterSourceData::getId
				);

		System.out.println(
				"[GameDataStore] monster sources loaded = "
				+ monsterSources.size()
		);
	}


	private void loadMonsterSourceLocalizations() {

		monsterSourceLocalizationsEn =
				readJson(
						"game-data/localization/en/monster-sources.json",
						new TypeReference<
								Map<Integer, MonsterSourceLocalization>>() {},
						"영문 몬스터 획득처 localization 데이터를 로딩할 수 없습니다."
				);

		monsterSourceLocalizationsKo =
				readJson(
						"game-data/localization/ko/monster-sources.json",
						new TypeReference<
								Map<Integer, MonsterSourceLocalization>>() {},
						"한글 몬스터 획득처 localization 데이터를 로딩할 수 없습니다."
				);

		monsterSourceLocalizationsEn =
				Collections.unmodifiableMap(
						monsterSourceLocalizationsEn
				);

		monsterSourceLocalizationsKo =
				Collections.unmodifiableMap(
						monsterSourceLocalizationsKo
				);

		System.out.println(
				"[GameDataStore] monster source localization EN loaded = "
				+ monsterSourceLocalizationsEn.size()
		);

		System.out.println(
				"[GameDataStore] monster source localization KO loaded = "
				+ monsterSourceLocalizationsKo.size()
		);
	}


	// =========================================================
	// 공통 JSON 로더
	// =========================================================

	private <T> T readJson(
			String path,
			TypeReference<T> typeReference,
			String errorMessage) {

		ClassPathResource resource =
				new ClassPathResource(
						path
				);

		try (InputStream inputStream =
				resource.getInputStream()) {

			return jsonMapper.readValue(
					inputStream,
					typeReference
			);
		}
		catch (Exception e) {

			throw new IllegalStateException(
					errorMessage
					+ " path="
					+ path,
					e
			);
		}
	}


	private <T> Map<Integer, T> toIdMap(
			List<T> list,
			Function<T, Integer> idExtractor) {

		return list.stream()
				.collect(
						Collectors.toUnmodifiableMap(
								idExtractor,
								Function.identity()
						)
				);
	}


	// =========================================================
	// 조회
	// =========================================================

	public Map<Integer, MonsterData> getMonsters() {
		return monsters;
	}


	public MonsterData getMonster(
			int monsterId) {

		return monsters.get(
				monsterId
		);
	}


	public Map<Integer, FamilyData> getFamilies() {
		return families;
	}


	public FamilyData getFamily(
			int familyId) {

		return families.get(
				familyId
		);
	}


	public Map<Integer, MonsterLocalization>
			getMonsterLocalizationsEn() {

		return monsterLocalizationsEn;
	}


	public Map<Integer, MonsterLocalization>
			getMonsterLocalizationsKo() {

		return monsterLocalizationsKo;
	}


	public MonsterLocalization getMonsterLocalizationEn(
			int monsterId) {

		return monsterLocalizationsEn.get(
				monsterId
		);
	}


	public MonsterLocalization getMonsterLocalizationKo(
			int monsterId) {

		return monsterLocalizationsKo.get(
				monsterId
		);
	}


	public Map<Integer, SkillData> getSkills() {
		return skills;
	}


	public SkillData getSkill(
			int skillId) {

		return skills.get(
				skillId
		);
	}


	public SkillLocalization getSkillLocalizationEn(
			int skillId) {

		return skillLocalizationsEn.get(
				skillId
		);
	}


	public SkillLocalization getSkillLocalizationKo(
			int skillId) {

		return skillLocalizationsKo.get(
				skillId
		);
	}


	public Map<Integer, SkillEffectData>
			getSkillEffects() {

		return skillEffects;
	}


	public SkillEffectData getSkillEffect(
			int effectId) {

		return skillEffects.get(
				effectId
		);
	}


	public SkillEffectLocalization
			getSkillEffectLocalizationEn(
					int effectId) {

		return skillEffectLocalizationsEn.get(
				effectId
		);
	}


	public SkillEffectLocalization
			getSkillEffectLocalizationKo(
					int effectId) {

		return skillEffectLocalizationsKo.get(
				effectId
		);
	}


	public LeaderSkillData getLeaderSkill(
			int leaderSkillId) {

		return leaderSkills.get(
				leaderSkillId
		);
	}


	public MonsterSourceData getMonsterSource(
			int sourceId) {

		return monsterSources.get(
				sourceId
		);
	}


	public MonsterSourceLocalization
			getMonsterSourceLocalizationEn(
					int sourceId) {

		return monsterSourceLocalizationsEn.get(
				sourceId
		);
	}


	public MonsterSourceLocalization
			getMonsterSourceLocalizationKo(
					int sourceId) {

		return monsterSourceLocalizationsKo.get(
				sourceId
		);
	}


	// =========================================================
	// 전체 데이터 검증
	// =========================================================

	private void validateData() {

		validateFamilies();
		validateMonsters();
		validateSkills();
		validateSkillEffects();
		validateLeaderSkills();
		validateMonsterSources();
		validateEnglishLocalizationCoverage();
	}


	// =========================================================
	// Family 검증
	// =========================================================

	private void validateFamilies() {

		for (FamilyData family :
				families.values()) {

			validateSourceIds(
					"Family",
					family.getId(),
					family.getSourceIds()
			);

			if (family.getMonsterIds()
					== null) {

				throw new IllegalStateException(
						"Family monsterIds가 null입니다."
						+ " familyId="
						+ family.getId()
				);
			}

			for (Integer monsterId :
					family.getMonsterIds()) {

				MonsterData monster =
						monsters.get(
								monsterId
						);

				if (monster == null) {

					throw new IllegalStateException(
							"Family가 존재하지 않는 Monster를 참조합니다."
							+ " familyId="
							+ family.getId()
							+ ", monsterId="
							+ monsterId
					);
				}

				if (!Integer.valueOf(
						family.getId()
				)
				.equals(
						monster.getFamilyId()
				)) {

					throw new IllegalStateException(
							"Family/Monster familyId가 일치하지 않습니다."
							+ " familyId="
							+ family.getId()
							+ ", monsterId="
							+ monsterId
					);
				}
			}
		}
	}


	// =========================================================
	// Monster 검증
	// =========================================================

	private void validateMonsters() {

		for (MonsterData monster :
				monsters.values()) {

			int monsterId =
					monster.getId();

			validateSourceIds(
					"Monster",
					monsterId,
					monster.getSourceIds()
			);

			if (monster.getElement()
					== null) {

				throw new IllegalStateException(
						"Monster element 데이터가 없습니다."
						+ " monsterId="
						+ monsterId
				);
			}

			if (monster.getArchetype()
					== null) {

				throw new IllegalStateException(
						"Monster archetype 데이터가 없습니다."
						+ " monsterId="
						+ monsterId
				);
			}

			if (monster.getEntityType()
					== null
					|| monster.getEntityType()
							== MonsterEntityType.UNKNOWN) {

				throw new IllegalStateException(
						"Monster entityType이 확정되지 않았습니다."
						+ " monsterId="
						+ monsterId
						+ ", entityType="
						+ monster.getEntityType()
				);
			}

			if (monster.getAwakening()
					== null
					|| monster.getAwakening()
							.getStage()
							== null) {

				throw new IllegalStateException(
						"Monster awakening 데이터가 없습니다."
						+ " monsterId="
						+ monsterId
				);
			}

			if (monster.getBaseStats()
					== null) {

				throw new IllegalStateException(
						"Monster baseStats 데이터가 없습니다."
						+ " monsterId="
						+ monsterId
				);
			}

			if (monster.getStats()
					== null) {

				throw new IllegalStateException(
						"Monster stats 데이터가 없습니다."
						+ " monsterId="
						+ monsterId
				);
			}

			if (monster.getFlags()
					== null) {

				throw new IllegalStateException(
						"Monster flags 데이터가 없습니다."
						+ " monsterId="
						+ monsterId
				);
			}

			validateMonsterFamily(
					monster
			);

			validateAwakeningReferences(
					monster
			);

			validateMonsterSkills(
					monster
			);

			validateLeaderSkillReference(
					monster
			);

			validateMonsterSourceReferences(
					monster
			);

			validateTransformReference(
					monster
			);

			validatePlayableStats(
					monster
			);
		}
	}


	private void validateMonsterFamily(
			MonsterData monster) {

		Integer familyId =
				monster.getFamilyId();

		if (familyId == null) {
			return;
		}

		FamilyData family =
				families.get(
						familyId
				);

		if (family == null) {

			throw new IllegalStateException(
					"존재하지 않는 Family ID를 참조합니다."
					+ " monsterId="
					+ monster.getId()
					+ ", familyId="
					+ familyId
			);
		}

		if (family.getMonsterIds()
				== null
				|| !family.getMonsterIds()
						.contains(
								monster.getId()
						)) {

			throw new IllegalStateException(
					"Monster가 자신의 Family에 포함되어 있지 않습니다."
					+ " monsterId="
					+ monster.getId()
					+ ", familyId="
					+ familyId
			);
		}
	}


	private void validateAwakeningReferences(
			MonsterData monster) {

		Integer previousFormId =
				monster.getAwakening()
						.getPreviousFormId();

		if (previousFormId != null
				&& !monsters.containsKey(
						previousFormId
				)) {

			throw new IllegalStateException(
					"존재하지 않는 previousFormId를 참조합니다."
					+ " monsterId="
					+ monster.getId()
					+ ", previousFormId="
					+ previousFormId
			);
		}

		Integer nextFormId =
				monster.getAwakening()
						.getNextFormId();

		if (nextFormId != null
				&& !monsters.containsKey(
						nextFormId
				)) {

			throw new IllegalStateException(
					"존재하지 않는 nextFormId를 참조합니다."
					+ " monsterId="
					+ monster.getId()
					+ ", nextFormId="
					+ nextFormId
			);
		}
	}


	private void validateMonsterSkills(
			MonsterData monster) {

		if (monster.getSkills()
				== null) {

			throw new IllegalStateException(
					"Monster skills가 null입니다."
					+ " monsterId="
					+ monster.getId()
			);
		}

		for (MonsterSkillRef skillRef :
				monster.getSkills()) {

			if (skillRef == null) {

				throw new IllegalStateException(
						"Monster skill reference가 null입니다."
						+ " monsterId="
						+ monster.getId()
				);
			}

			SkillData skill =
					skills.get(
							skillRef.getSkillId()
					);

			if (skill == null) {

				throw new IllegalStateException(
						"존재하지 않는 Skill ID를 참조합니다."
						+ " monsterId="
						+ monster.getId()
						+ ", skillId="
						+ skillRef.getSkillId()
				);
			}

			if (skillRef.getSlot()
					!= skill.getSlot()) {

				throw new IllegalStateException(
						"Monster skill slot과 Skill slot이 일치하지 않습니다."
						+ " monsterId="
						+ monster.getId()
						+ ", skillId="
						+ skill.getId()
						+ ", monsterSlot="
						+ skillRef.getSlot()
						+ ", skillSlot="
						+ skill.getSlot()
				);
			}
		}
	}


	private void validateLeaderSkillReference(
			MonsterData monster) {

		Integer leaderSkillId =
				monster.getLeaderSkillId();

		if (leaderSkillId == null) {
			return;
		}

		if (!leaderSkills.containsKey(
				leaderSkillId
		)) {

			throw new IllegalStateException(
					"존재하지 않는 LeaderSkill ID를 참조합니다."
					+ " monsterId="
					+ monster.getId()
					+ ", leaderSkillId="
					+ leaderSkillId
			);
		}
	}


	private void validateMonsterSourceReferences(
			MonsterData monster) {

		if (monster.getObtainSourceIds()
				== null) {

			throw new IllegalStateException(
					"Monster obtainSourceIds가 null입니다."
					+ " monsterId="
					+ monster.getId()
			);
		}

		for (Integer sourceId :
				monster.getObtainSourceIds()) {

			if (sourceId == null
					|| !monsterSources.containsKey(
							sourceId
					)) {

				throw new IllegalStateException(
						"존재하지 않는 MonsterSource ID를 참조합니다."
						+ " monsterId="
						+ monster.getId()
						+ ", sourceId="
						+ sourceId
				);
			}
		}
	}


	private void validateTransformReference(
			MonsterData monster) {

		Integer transformsToId =
				monster.getTransformsToId();

		if (transformsToId == null) {
			return;
		}

		if (!monsters.containsKey(
				transformsToId
		)) {

			throw new IllegalStateException(
					"존재하지 않는 transformsToId를 참조합니다."
					+ " monsterId="
					+ monster.getId()
					+ ", transformsToId="
					+ transformsToId
			);
		}
	}


	private void validatePlayableStats(
			MonsterData monster) {

		if (monster.getEntityType()
				!= MonsterEntityType.PLAYABLE) {

			return;
		}

		if (monster.getBaseStats().getHp()
				== null
				|| monster.getBaseStats()
						.getAttack()
						== null
				|| monster.getBaseStats()
						.getDefense()
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
						== null) {

			throw new IllegalStateException(
					"PLAYABLE Monster에 누락된 스탯이 있습니다."
					+ " monsterId="
					+ monster.getId()
			);
		}
	}


	// =========================================================
	// Skill 검증
	// =========================================================

	private void validateSkills() {

		for (SkillData skill :
				skills.values()) {

			int skillId =
					skill.getId();

			validateSourceIds(
					"Skill",
					skillId,
					skill.getSourceIds()
			);

			if (skill.getFlags()
					== null) {

				throw new IllegalStateException(
						"Skill flags 데이터가 없습니다."
						+ " skillId="
						+ skillId
				);
			}

			if (skill.getScaling()
					== null
					|| skill.getScaling()
							.getExpression()
							== null
					|| skill.getScaling()
							.getStats()
							== null) {

				throw new IllegalStateException(
						"Skill scaling 데이터가 없습니다."
						+ " skillId="
						+ skillId
				);
			}

			if (skill.getEffects()
					== null) {

				throw new IllegalStateException(
						"Skill effects가 null입니다."
						+ " skillId="
						+ skillId
				);
			}

			for (SkillEffectRefData effectRef :
					skill.getEffects()) {

				if (effectRef == null
						|| !skillEffects.containsKey(
								effectRef.getEffectId()
						)) {

					throw new IllegalStateException(
							"존재하지 않는 SkillEffect를 참조합니다."
							+ " skillId="
							+ skillId
							+ ", effectId="
							+ (
									effectRef != null
											? effectRef.getEffectId()
											: null
							)
					);
				}

				if (effectRef.getFlags()
						== null) {

					throw new IllegalStateException(
							"SkillEffect reference flags가 없습니다."
							+ " skillId="
							+ skillId
							+ ", effectId="
							+ effectRef.getEffectId()
					);
				}
			}

			if (skill.getLevelUps()
					== null) {

				throw new IllegalStateException(
						"Skill levelUps가 null입니다."
						+ " skillId="
						+ skillId
				);
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

				if (levelUp == null
						|| levelUp.getLevel()
								!= index + 2) {

					throw new IllegalStateException(
							"Skill levelUp 순서가 올바르지 않습니다."
							+ " skillId="
							+ skillId
							+ ", index="
							+ index
					);
				}
			}

			Integer relatedSkillId =
					skill.getRelatedSkillId();

			if (relatedSkillId != null
					&& !skills.containsKey(
							relatedSkillId
					)) {

				throw new IllegalStateException(
						"존재하지 않는 relatedSkillId를 참조합니다."
						+ " skillId="
						+ skillId
						+ ", relatedSkillId="
						+ relatedSkillId
				);
			}
		}
	}


	// =========================================================
	// SkillEffect / LeaderSkill / Source 검증
	// =========================================================

	private void validateSkillEffects() {

		for (SkillEffectData effect :
				skillEffects.values()) {

			validateSourceIds(
					"SkillEffect",
					effect.getId(),
					effect.getSourceIds()
			);

			if (effect.getType()
					== null
					|| effect.getFlags()
							== null) {

				throw new IllegalStateException(
						"SkillEffect 필수 데이터가 없습니다."
						+ " effectId="
						+ effect.getId()
				);
			}
		}
	}


	private void validateLeaderSkills() {

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

			validateSourceIds(
					"LeaderSkill",
					leaderSkill.getId(),
					leaderSkill.getSourceIds()
			);

			if (!validStats.contains(
					leaderSkill.getStat()
			)) {

				throw new IllegalStateException(
						"잘못된 LeaderSkill stat입니다."
						+ " leaderSkillId="
						+ leaderSkill.getId()
						+ ", stat="
						+ leaderSkill.getStat()
				);
			}

			if (!validAreas.contains(
					leaderSkill.getArea()
			)) {

				throw new IllegalStateException(
						"잘못된 LeaderSkill area입니다."
						+ " leaderSkillId="
						+ leaderSkill.getId()
						+ ", area="
						+ leaderSkill.getArea()
				);
			}

			if (leaderSkill.getElement()
					!= null
					&& !validElements.contains(
							leaderSkill.getElement()
					)) {

				throw new IllegalStateException(
						"잘못된 LeaderSkill element입니다."
						+ " leaderSkillId="
						+ leaderSkill.getId()
						+ ", element="
						+ leaderSkill.getElement()
				);
			}
		}
	}


	private void validateMonsterSources() {

		for (MonsterSourceData source :
				monsterSources.values()) {

			validateSourceIds(
					"MonsterSource",
					source.getId(),
					source.getSourceIds()
			);
		}
	}


	// =========================================================
	// Localization 검증
	// =========================================================

	private void validateEnglishLocalizationCoverage() {

		validateLocalizationCoverage(
				"Monster",
				monsters,
				monsterLocalizationsEn
		);

		validateLocalizationCoverage(
				"Skill",
				skills,
				skillLocalizationsEn
		);

		validateLocalizationCoverage(
				"SkillEffect",
				skillEffects,
				skillEffectLocalizationsEn
		);

		validateLocalizationCoverage(
				"MonsterSource",
				monsterSources,
				monsterSourceLocalizationsEn
		);
	}


	private void validateLocalizationCoverage(
			String type,
			Map<Integer, ?> entities,
			Map<Integer, ?> localizations) {

		for (Integer id :
				entities.keySet()) {

			if (!localizations.containsKey(
					id
			)) {

				throw new IllegalStateException(
						type
						+ " 영문 localization이 없습니다."
						+ " id="
						+ id
				);
			}
		}

		for (Integer id :
				localizations.keySet()) {

			if (!entities.containsKey(
					id
			)) {

				throw new IllegalStateException(
						type
						+ " 영문 localization이 고아 데이터입니다."
						+ " id="
						+ id
				);
			}
		}
	}


	// =========================================================
	// 공통 검증
	// =========================================================

	private void validateSourceIds(
			String type,
			int id,
			SourceIds sourceIds) {

		if (id <= 0) {

			throw new IllegalStateException(
					type
					+ " 내부 ID가 올바르지 않습니다."
					+ " id="
					+ id
			);
		}

		if (sourceIds == null
				|| sourceIds.getSwarfarm()
						== null
				|| sourceIds.getSwarfarm()
						<= 0) {

			throw new IllegalStateException(
					type
					+ " sourceIds.swarfarm이 없습니다."
					+ " id="
					+ id
			);
		}
	}
}