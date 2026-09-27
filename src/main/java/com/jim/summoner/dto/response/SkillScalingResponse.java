package com.jim.summoner.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SkillScalingResponse {

	private String formula;

	/*
	 * SWARFARM multiplier_formula_raw을
	 * 구조화해서 보존한 표현.
	 *
	 * 예:
	 * [
	 *   ["ATK", "*", 3.6]
	 * ]
	 */
	private List<List<Object>> expression;

	private List<String> stats;
}