package com.jim.summoner.data.model;

import lombok.Data;

@Data
public class SkillEffectRefData {

	/*
	 * data-source/id-map/skill-effects.json을 통해
	 * 변환된 우리 내부 SkillEffect ID.
	 */
	private int effectId;

	/*
	 * 실제 raw에서 null이 존재하므로
	 * primitive int를 사용하지 않는다.
	 */
	private Integer chance;

	private Integer quantity;

	private SkillEffectRefFlags flags;

	/*
	 * SWARFARM의 effect reference note.
	 *
	 * 구조화하기 어려운 조건 설명이 들어 있으므로
	 * 현재는 원문을 그대로 보존한다.
	 */
	private String note;
}