package com.jim.summoner.data.model;

import java.util.List;

import lombok.Data;

@Data
public class SkillData {

	private int id;

	private SourceIds sourceIds;

	/*
	 * SWARFARM에는 특수/미사용 스킬에서
	 * slot = -1인 데이터가 존재한다.
	 *
	 * 원본 의미를 임의로 바꾸지 않고 그대로 보존한다.
	 */
	private int slot;

	/*
	 * cooltime은 실제 raw에서 null일 수 있다.
	 *
	 * null:
	 * 원본에서 쿨타임 값이 없음
	 *
	 * 숫자:
	 * 명시적인 쿨타임 값
	 */
	private Integer cooldown;

	/*
	 * hits 역시 0 및 음수 sentinel 값이 존재하므로
	 * 원본 값을 그대로 보존한다.
	 */
	private int hits;

	private SkillFlags flags;

	private SkillScaling scaling;

	private List<SkillEffectRefData> effects =
			List.of();

	private int maxLevel;

	private List<SkillLevelUpData> levelUps =
			List.of();

	private Integer relatedSkillId;

	private AssetData assets;
}