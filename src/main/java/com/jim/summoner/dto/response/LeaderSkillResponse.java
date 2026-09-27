package com.jim.summoner.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeaderSkillResponse {

	private String stat;
	private int amount;

	private String area;
	private String element;
}