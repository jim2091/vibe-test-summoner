package com.jim.summoner.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.jim.summoner.data.GameDataStore;
import com.jim.summoner.data.model.FamilyData;
import com.jim.summoner.data.model.LeaderSkillData;
import com.jim.summoner.data.model.MonsterData;
import com.jim.summoner.data.model.MonsterLocalization;
import com.jim.summoner.data.model.MonsterSourceData;
import com.jim.summoner.data.model.MonsterSourceLocalization;
import com.jim.summoner.data.model.SkillData;
import com.jim.summoner.data.model.SkillEffectData;
import com.jim.summoner.data.model.SkillEffectLocalization;
import com.jim.summoner.data.model.SkillEffectRefData;
import com.jim.summoner.data.model.SkillLevelUpData;
import com.jim.summoner.data.model.SkillLocalization;
import com.jim.summoner.data.type.MonsterEntityType;
import com.jim.summoner.dto.request.MonsterSearchRequest;
import com.jim.summoner.dto.request.MonsterSortType;
import com.jim.summoner.dto.request.SortDirection;
import com.jim.summoner.dto.response.LeaderSkillResponse;
import com.jim.summoner.dto.response.MonsterAwakeningResponse;
import com.jim.summoner.dto.response.MonsterBaseStatsResponse;
import com.jim.summoner.dto.response.MonsterDetailResponse;
import com.jim.summoner.dto.response.MonsterFlagsResponse;
import com.jim.summoner.dto.response.MonsterListItemResponse;
import com.jim.summoner.dto.response.MonsterListResponse;
import com.jim.summoner.dto.response.MonsterSkillResponse;
import com.jim.summoner.dto.response.MonsterSourceResponse;
import com.jim.summoner.dto.response.MonsterStatsResponse;
import com.jim.summoner.dto.response.PageResponse;
import com.jim.summoner.dto.response.SkillEffectFlagsResponse;
import com.jim.summoner.dto.response.SkillEffectResponse;
import com.jim.summoner.dto.response.SkillLevelUpResponse;
import com.jim.summoner.dto.response.SkillScalingResponse;
import com.jim.summoner.error.GameDataNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MonsterServiceImpl implements MonsterService {

	private final GameDataStore gameDataStore;


	// =========================================================
	// Monster 목록
	// =========================================================

	@Override
	public MonsterListResponse getMonsters(
			MonsterSearchRequest request) {

		Stream<MonsterData> stream =
				gameDataStore.getMonsters()
						.values()
						.stream()
						.filter(
								monster ->
										monster.getEntityType()
												== MonsterEntityType.PLAYABLE
						);


		stream =
				applyKeywordFilter(
						stream,
						request.getKeyword()
				);


		stream =
				applyFilters(
						stream,
						request
				);


		Comparator<MonsterData> comparator =
				createComparator(
						request.getSort(),
						request.getDirection()
				);


		List<MonsterData> filtered =
				stream.sorted(
						comparator
				)
				.toList();


		return paginate(
				filtered,
				request.getPage(),
				request.getSize()
		);
	}


	// =========================================================
	// 검색
	// =========================================================

	private Stream<MonsterData> applyKeywordFilter(
			Stream<MonsterData> stream,
			String keyword) {

		if (keyword == null
				|| keyword.isBlank()) {

			return stream;
		}


		String normalizedKeyword =
				keyword.trim()
						.toLowerCase(
								Locale.ROOT
						);


		return stream.filter(
				monster -> {

					MonsterLocalization ko =
							gameDataStore
									.getMonsterLocalizationKo(
											monster.getId()
									);


					MonsterLocalization en =
							gameDataStore
									.getMonsterLocalizationEn(
											monster.getId()
									);


					return containsIgnoreCase(
							ko != null
									? ko.getName()
									: null,
							normalizedKeyword
					)
					||
					containsIgnoreCase(
							en != null
									? en.getName()
									: null,
							normalizedKeyword
					);
				}
		);
	}


	private boolean containsIgnoreCase(
			String value,
			String normalizedKeyword) {

		if (value == null) {

			return false;
		}


		return value
				.toLowerCase(
						Locale.ROOT
				)
				.contains(
						normalizedKeyword
				);
	}


	// =========================================================
	// 필터
	// =========================================================

	private Stream<MonsterData> applyFilters(
			Stream<MonsterData> stream,
			MonsterSearchRequest request) {

		if (request.getElements()
				!= null
				&& !request.getElements()
						.isEmpty()) {

			stream =
					stream.filter(
							monster ->
									request.getElements()
											.contains(
													monster.getElement()
											)
					);
		}


		if (request.getStars()
				!= null
				&& !request.getStars()
						.isEmpty()) {

			stream =
					stream.filter(
							monster ->
									request.getStars()
											.contains(
													monster.getNaturalStars()
											)
					);
		}


		if (request.getArchetypes()
				!= null
				&& !request.getArchetypes()
						.isEmpty()) {

			stream =
					stream.filter(
							monster ->
									request.getArchetypes()
											.contains(
													monster.getArchetype()
											)
					);
		}


		if (request.getAwakeningStages()
				!= null
				&& !request.getAwakeningStages()
						.isEmpty()) {

			stream =
					stream.filter(
							monster ->
									request.getAwakeningStages()
											.contains(
													monster.getAwakening()
															.getStage()
											)
					);
		}


		if (request.getObtainable()
				!= null) {

			stream =
					stream.filter(
							monster ->
									monster.isObtainable()
											== request.getObtainable()
					);
		}


		return stream;
	}


	// =========================================================
	// 정렬
	// =========================================================

	private Comparator<MonsterData> createComparator(
			MonsterSortType sort,
			SortDirection direction) {

		Comparator<MonsterData> comparator;


		switch (sort) {

		case NAME:

			comparator =
					Comparator.comparing(
							this::getDisplayName,
							String.CASE_INSENSITIVE_ORDER
					);

			break;


		case STARS:

			comparator =
					Comparator.comparingInt(
							MonsterData::getNaturalStars
					);

			break;


		case DEFAULT:
		default:

			comparator =
					Comparator.comparingInt(
							MonsterData::getId
					);

			break;
		}


		if (direction
				== SortDirection.DESC) {

			comparator =
					comparator.reversed();
		}


		return comparator;
	}


	private String getDisplayName(
			MonsterData monster) {

		MonsterLocalization ko =
				gameDataStore
						.getMonsterLocalizationKo(
								monster.getId()
						);


		if (ko != null
				&& ko.getName()
						!= null
				&& !ko.getName()
						.isBlank()) {

			return ko.getName();
		}


		MonsterLocalization en =
				gameDataStore
						.getMonsterLocalizationEn(
								monster.getId()
						);


		if (en != null
				&& en.getName()
						!= null) {

			return en.getName();
		}


		return "";
	}


	// =========================================================
	// 페이지네이션
	// =========================================================

	private MonsterListResponse paginate(
			List<MonsterData> monsters,
			int page,
			int size) {

		int totalElements =
				monsters.size();


		int totalPages =
				(int) Math.ceil(
						(double) totalElements
						/ size
				);


		int fromIndex =
				(page - 1)
				* size;


		int toIndex =
				Math.min(
						fromIndex + size,
						totalElements
				);


		List<MonsterData> pageItems;


		if (fromIndex
				>= totalElements) {

			pageItems =
					List.of();
		}
		else {

			pageItems =
					monsters.subList(
							fromIndex,
							toIndex
					);
		}


		List<MonsterListItemResponse> items =
				pageItems.stream()
						.map(
								this::toListItem
						)
						.toList();


		PageResponse pageResponse =
				PageResponse.builder()

						.number(
								page
						)

						.size(
								size
						)

						.totalElements(
								totalElements
						)

						.totalPages(
								totalPages
						)

						.build();


		return MonsterListResponse.builder()

				.items(
						items
				)

				.page(
						pageResponse
				)

				.build();
	}


	// =========================================================
	// 목록 DTO
	// =========================================================

	private MonsterListItemResponse toListItem(
			MonsterData monster) {

		MonsterLocalization ko =
				gameDataStore
						.getMonsterLocalizationKo(
								monster.getId()
						);


		MonsterLocalization en =
				gameDataStore
						.getMonsterLocalizationEn(
								monster.getId()
						);


		return MonsterListItemResponse.builder()

				.id(
						monster.getId()
				)

				.nameKo(
						ko != null
								? ko.getName()
								: null
				)

				.nameEn(
						en != null
								? en.getName()
								: null
				)

				.element(
						monster.getElement()
				)

				.archetype(
						monster.getArchetype()
				)

				.naturalStars(
						monster.getNaturalStars()
				)

				.awakeningStage(
						monster.getAwakening()
								.getStage()
				)

				.iconUrl(
						makeMonsterIconUrl(
								monster
						)
				)

				.build();
	}


	// =========================================================
	// Monster 상세
	// =========================================================

	@Override
	public MonsterDetailResponse getMonster(
			int monsterId) {

		MonsterData monster =
				gameDataStore.getMonster(
						monsterId
				);


		if (monster == null) {

			throw new GameDataNotFoundException(
					"몬스터 정보를 찾을 수 없습니다."
			);
		}


		MonsterLocalization ko =
				gameDataStore
						.getMonsterLocalizationKo(
								monsterId
						);


		MonsterLocalization en =
				gameDataStore
						.getMonsterLocalizationEn(
								monsterId
						);


		List<MonsterSkillResponse> skills =
				monster.getSkills()
						.stream()

						.map(
								skillRef ->
										toSkillResponse(
												skillRef.getSkillId(),
												skillRef.getSlot()
										)
						)

						.toList();


		List<MonsterSourceResponse> sources =
				monster.getObtainSourceIds()
						.stream()

						.map(
								this::toSourceResponse
						)

						.toList();


		return MonsterDetailResponse.builder()

				.id(
						monster.getId()
				)

				.nameKo(
						ko != null
								? ko.getName()
								: null
				)

				.nameEn(
						en != null
								? en.getName()
								: null
				)

				.element(
						monster.getElement()
				)

				.archetype(
						monster.getArchetype()
				)

				.entityType(
						monster.getEntityType()
				)

				.familyId(
						monster.getFamilyId()
				)

				.familyMonsterIds(
						getFamilyMonsterIds(
								monster
						)
				)

				.baseStars(
						monster.getBaseStars()
				)

				.naturalStars(
						monster.getNaturalStars()
				)

				.awakening(
						toAwakeningResponse(
								monster,
								ko,
								en
						)
				)

				.baseStats(
						toBaseStatsResponse(
								monster
						)
				)

				.stats(
						toStatsResponse(
								monster
						)
				)

				.skills(
						skills
				)

				.skillUpsToMax(
						monster.getSkillUpsToMax()
				)

				.leaderSkill(
						toLeaderSkillResponse(
								monster.getLeaderSkillId()
						)
				)

				.obtainable(
						monster.isObtainable()
				)

				.obtainSources(
						sources
				)

				.flags(
						toMonsterFlagsResponse(
								monster
						)
				)

				.transformsToId(
						monster.getTransformsToId()
				)

				.iconUrl(
						makeMonsterIconUrl(
								monster
						)
				)

				.build();
	}


	// =========================================================
	// Family
	// =========================================================

	private List<Integer> getFamilyMonsterIds(
			MonsterData monster) {

		Integer familyId =
				monster.getFamilyId();


		if (familyId == null) {

			return List.of();
		}


		FamilyData family =
				gameDataStore.getFamily(
						familyId
				);


		if (family == null
				|| family.getMonsterIds()
						== null) {

			return List.of();
		}


		return family.getMonsterIds();
	}


	// =========================================================
	// Base Stats
	// =========================================================

	private MonsterBaseStatsResponse toBaseStatsResponse(
			MonsterData monster) {

		if (monster.getBaseStats()
				== null) {

			return null;
		}


		return MonsterBaseStatsResponse.builder()

				.hp(
						monster.getBaseStats()
								.getHp()
				)

				.attack(
						monster.getBaseStats()
								.getAttack()
				)

				.defense(
						monster.getBaseStats()
								.getDefense()
				)

				.build();
	}


	// =========================================================
	// Max Stats
	// =========================================================

	private MonsterStatsResponse toStatsResponse(
			MonsterData monster) {

		if (monster.getStats()
				== null) {

			return null;
		}


		return MonsterStatsResponse.builder()

				.hp(
						monster.getStats()
								.getHp()
				)

				.attack(
						monster.getStats()
								.getAttack()
				)

				.defense(
						monster.getStats()
								.getDefense()
				)

				.speed(
						monster.getStats()
								.getSpeed()
				)

				.critRate(
						monster.getStats()
								.getCritRate()
				)

				.critDamage(
						monster.getStats()
								.getCritDamage()
				)

				.resistance(
						monster.getStats()
								.getResistance()
				)

				.accuracy(
						monster.getStats()
								.getAccuracy()
				)

				.build();
	}


	// =========================================================
	// Awakening
	// =========================================================

	private MonsterAwakeningResponse toAwakeningResponse(
			MonsterData monster,
			MonsterLocalization ko,
			MonsterLocalization en) {

		if (monster.getAwakening()
				== null) {

			return null;
		}


		return MonsterAwakeningResponse.builder()

				.stage(
						monster.getAwakening()
								.getStage()
				)

				.canAwaken(
						monster.getAwakening()
								.isCanAwaken()
				)

				.previousFormId(
						monster.getAwakening()
								.getPreviousFormId()
				)

				.nextFormId(
						monster.getAwakening()
								.getNextFormId()
				)

				.bonusKo(
						ko != null
								? ko.getAwakeningBonus()
								: null
				)

				.bonusEn(
						en != null
								? en.getAwakeningBonus()
								: null
				)

				.build();
	}


	// =========================================================
	// Monster Flags
	// =========================================================

	private MonsterFlagsResponse toMonsterFlagsResponse(
			MonsterData monster) {

		if (monster.getFlags()
				== null) {

			return null;
		}


		return MonsterFlagsResponse.builder()

				.fusionFood(
						monster.getFlags()
								.isFusionFood()
				)

				.homunculus(
						monster.getFlags()
								.isHomunculus()
				)

				.build();
	}


	// =========================================================
	// Skill
	// =========================================================

	private MonsterSkillResponse toSkillResponse(
			int skillId,
			int slot) {

		SkillData skill =
				gameDataStore.getSkill(
						skillId
				);


		if (skill == null) {

			throw new IllegalStateException(
					"존재하지 않는 Skill ID를 참조합니다."
					+ " skillId="
					+ skillId
			);
		}


		SkillLocalization ko =
				gameDataStore
						.getSkillLocalizationKo(
								skillId
						);


		SkillLocalization en =
				gameDataStore
						.getSkillLocalizationEn(
								skillId
						);


		List<SkillLevelUpResponse> levelUps =
				toSkillLevelUpResponses(
						skill,
						ko,
						en
				);


		List<SkillEffectResponse> effects =
				skill.getEffects()
						.stream()

						.map(
								this::toSkillEffectResponse
						)

						.toList();


		return MonsterSkillResponse.builder()

				.id(
						skill.getId()
				)

				.slot(
						slot
				)

				.nameKo(
						ko != null
								? ko.getName()
								: null
				)

				.nameEn(
						en != null
								? en.getName()
								: null
				)

				.descriptionKo(
						ko != null
								? ko.getDescription()
								: null
				)

				.descriptionEn(
						en != null
								? en.getDescription()
								: null
				)

				.cooldown(
						skill.getCooldown()
				)

				.hits(
						skill.getHits()
				)

				.passive(
						skill.getFlags()
								.isPassive()
				)

				.aoe(
						skill.getFlags()
								.isAoe()
				)

				.randomTarget(
						skill.getFlags()
								.isRandomTarget()
				)

				.scaling(
						toSkillScalingResponse(
								skill
						)
				)

				.maxLevel(
						skill.getMaxLevel()
				)

				.levelUps(
						levelUps
				)

				.effects(
						effects
				)

				.relatedSkillId(
						skill.getRelatedSkillId()
				)

				.iconUrl(
						makeSkillIconUrl(
								skill
						)
				)

				.build();
	}


	// =========================================================
	// Skill Scaling
	// =========================================================

	private SkillScalingResponse toSkillScalingResponse(
			SkillData skill) {

		if (skill.getScaling()
				== null) {

			return null;
		}


		return SkillScalingResponse.builder()

				.formula(
						skill.getScaling()
								.getFormula()
				)

				.expression(
						skill.getScaling()
								.getExpression()
				)

				.stats(
						skill.getScaling()
								.getStats()
				)

				.build();
	}


	// =========================================================
	// Skill Level Up
	// =========================================================

	private List<SkillLevelUpResponse> toSkillLevelUpResponses(
			SkillData skill,
			SkillLocalization ko,
			SkillLocalization en) {

		List<SkillLevelUpData> levelUps =
				skill.getLevelUps();


		if (levelUps == null
				|| levelUps.isEmpty()) {

			return List.of();
		}


		return IntStream.range(
				0,
				levelUps.size()
		)
		.mapToObj(
				index -> {

					SkillLevelUpData levelUp =
							levelUps.get(
									index
							);


					return SkillLevelUpResponse.builder()

							.level(
									levelUp.getLevel()
							)

							.effectTemplate(
									levelUp.getEffectTemplate()
							)

							.amount(
									levelUp.getAmount()
							)

							.descriptionKo(
									getLevelUpDescription(
											ko,
											index
									)
							)

							.descriptionEn(
									getLevelUpDescription(
											en,
											index
									)
							)

							.build();
				}
		)
		.toList();
	}


	private String getLevelUpDescription(
			SkillLocalization localization,
			int index) {

		if (localization == null
				|| localization.getLevelUpDescriptions()
						== null
				|| index < 0
				|| index >= localization
						.getLevelUpDescriptions()
						.size()) {

			return null;
		}


		return localization
				.getLevelUpDescriptions()
				.get(
						index
				);
	}


	// =========================================================
	// Skill Effect
	// =========================================================

	private SkillEffectResponse toSkillEffectResponse(
			SkillEffectRefData reference) {

		SkillEffectData effect =
				gameDataStore.getSkillEffect(
						reference.getEffectId()
				);


		if (effect == null) {

			throw new IllegalStateException(
					"존재하지 않는 SkillEffect ID를 참조합니다."
					+ " effectId="
					+ reference.getEffectId()
			);
		}


		SkillEffectLocalization ko =
				gameDataStore
						.getSkillEffectLocalizationKo(
								effect.getId()
						);


		SkillEffectLocalization en =
				gameDataStore
						.getSkillEffectLocalizationEn(
								effect.getId()
						);


		return SkillEffectResponse.builder()

				.id(
						effect.getId()
				)

				.nameKo(
						ko != null
								? ko.getName()
								: null
				)

				.nameEn(
						en != null
								? en.getName()
								: null
				)

				.descriptionKo(
						ko != null
								? ko.getDescription()
								: null
				)

				.descriptionEn(
						en != null
								? en.getDescription()
								: null
				)

				.type(
						effect.getType()
				)

				.buff(
						effect.getFlags()
								.isBuff()
				)

				.chance(
						reference.getChance()
				)

				.quantity(
						reference.getQuantity()
				)

				.flags(
						toSkillEffectFlagsResponse(
								reference
						)
				)

				.note(
						reference.getNote()
				)

				.iconUrl(
						makeSkillEffectIconUrl(
								effect
						)
				)

				.build();
	}


	private SkillEffectFlagsResponse toSkillEffectFlagsResponse(
			SkillEffectRefData reference) {

		if (reference.getFlags()
				== null) {

			return null;
		}


		return SkillEffectFlagsResponse.builder()

				.aoe(
						reference.getFlags()
								.isAoe()
				)

				.singleTarget(
						reference.getFlags()
								.isSingleTarget()
				)

				.selfEffect(
						reference.getFlags()
								.isSelfEffect()
				)

				.onCrit(
						reference.getFlags()
								.isOnCrit()
				)

				.onDeath(
						reference.getFlags()
								.isOnDeath()
				)

				.random(
						reference.getFlags()
								.isRandom()
				)

				.allTargets(
						reference.getFlags()
								.isAllTargets()
				)

				.selfHp(
						reference.getFlags()
								.isSelfHp()
				)

				.targetHp(
						reference.getFlags()
								.isTargetHp()
				)

				.damage(
						reference.getFlags()
								.isDamage()
				)

				.build();
	}


	// =========================================================
	// Leader Skill
	// =========================================================

	private LeaderSkillResponse toLeaderSkillResponse(
			Integer leaderSkillId) {

		if (leaderSkillId == null) {

			return null;
		}


		LeaderSkillData leaderSkill =
				gameDataStore.getLeaderSkill(
						leaderSkillId
				);


		if (leaderSkill == null) {

			throw new IllegalStateException(
					"존재하지 않는 LeaderSkill ID를 참조합니다."
					+ " id="
					+ leaderSkillId
			);
		}


		return LeaderSkillResponse.builder()

				.stat(
						leaderSkill.getStat()
				)

				.amount(
						leaderSkill.getAmount()
				)

				.area(
						leaderSkill.getArea()
				)

				.element(
						leaderSkill.getElement()
				)

				.build();
	}


	// =========================================================
	// Monster Source
	// =========================================================

	private MonsterSourceResponse toSourceResponse(
			int sourceId) {

		MonsterSourceData source =
				gameDataStore.getMonsterSource(
						sourceId
				);


		if (source == null) {

			throw new IllegalStateException(
					"존재하지 않는 MonsterSource ID입니다."
					+ " id="
					+ sourceId
			);
		}


		MonsterSourceLocalization ko =
				gameDataStore
						.getMonsterSourceLocalizationKo(
								sourceId
						);


		MonsterSourceLocalization en =
				gameDataStore
						.getMonsterSourceLocalizationEn(
								sourceId
						);


		return MonsterSourceResponse.builder()

				.id(
						source.getId()
				)

				.nameKo(
						ko != null
								? ko.getName()
								: null
				)

				.nameEn(
						en != null
								? en.getName()
								: null
				)

				.descriptionKo(
						ko != null
								? ko.getDescription()
								: null
				)

				.descriptionEn(
						en != null
								? en.getDescription()
								: null
				)

				.farmable(
						source.isFarmable()
				)

				.build();
	}


	// =========================================================
	// Asset URL
	// =========================================================

	private String makeMonsterIconUrl(
			MonsterData monster) {

		if (monster.getAssets()
				== null
				|| monster.getAssets()
						.getIconKey()
						== null
				|| monster.getAssets()
						.getIconKey()
						.isBlank()) {

			return null;
		}


		return "/game-assets/monsters/"
				+ monster.getAssets()
						.getIconKey()
				+ ".png";
	}


	private String makeSkillIconUrl(
			SkillData skill) {

		if (skill.getAssets()
				== null
				|| skill.getAssets()
						.getIconKey()
						== null
				|| skill.getAssets()
						.getIconKey()
						.isBlank()) {

			return null;
		}


		return "/game-assets/skills/"
				+ skill.getAssets()
						.getIconKey()
				+ ".png";
	}


	private String makeSkillEffectIconUrl(
			SkillEffectData effect) {

		if (effect.getAssets()
				== null
				|| effect.getAssets()
						.getIconKey()
						== null
				|| effect.getAssets()
						.getIconKey()
						.isBlank()) {

			return null;
		}


		return "/game-assets/effects/"
				+ effect.getAssets()
						.getIconKey()
				+ ".png";
	}
}