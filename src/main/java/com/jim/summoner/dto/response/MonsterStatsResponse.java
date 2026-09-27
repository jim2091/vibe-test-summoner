package com.jim.summoner.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonsterStatsResponse {

	private Integer hp;
	private Integer attack;
	private Integer defense;

	private Integer speed;

	private Integer critRate;
	private Integer critDamage;

	private Integer resistance;
	private Integer accuracy;
}