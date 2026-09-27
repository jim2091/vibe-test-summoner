package com.jim.summoner.data.model;

import lombok.Data;

@Data
public class SkillLevelUpData {

	/*
	 * 실제 스킬 레벨.
	 *
	 * raw upgrades[0] = level 2
	 * raw upgrades[1] = level 3
	 * ...
	 */
	private int level;

	/*
	 * SWARFARM 원본 패턴.
	 *
	 * 예:
	 * Damage +{0}%
	 * Effect Rate +{0}%
	 * Cooltime Turn -{0}
	 *
	 * 현재 단계에서는 자체 enum으로 재해석하지 않는다.
	 */
	private String effectTemplate;

	private int amount;
}