package com.jim.summoner.data.model;

import lombok.Data;

@Data
public class SkillEffectFlags {

	/*
	 * SWARFARM의 is_buff 값을 그대로 보존한다.
	 *
	 * type == BUFF와 항상 동일한 의미라고
	 * 가정하지 않는다.
	 */
	private boolean buff;
}