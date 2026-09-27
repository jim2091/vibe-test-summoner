package com.jim.summoner.data.model;

import com.jim.summoner.data.type.AwakeningStage;

import lombok.Data;

@Data
public class AwakeningData {

	private AwakeningStage stage;
	private boolean canAwaken;

	private Integer previousFormId;
	private Integer nextFormId;
}