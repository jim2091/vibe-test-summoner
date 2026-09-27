package com.jim.summoner.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonsterBaseStatsResponse {

	private Integer hp;
	private Integer attack;
	private Integer defense;
}