package com.jim.summoner.dto.response;

import com.jim.summoner.data.type.AwakeningStage;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonsterAwakeningResponse {

	private AwakeningStage stage;

	private boolean canAwaken;

	private Integer previousFormId;
	private Integer nextFormId;

	private String bonusKo;
	private String bonusEn;
}