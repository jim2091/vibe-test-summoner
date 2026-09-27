package com.jim.summoner.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonsterFlagsResponse {

	private boolean fusionFood;
	private boolean homunculus;
}