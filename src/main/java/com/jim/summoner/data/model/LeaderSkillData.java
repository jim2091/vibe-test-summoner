package com.jim.summoner.data.model;

import lombok.Data;

@Data
public class LeaderSkillData {

	private int id;

	private SourceIds sourceIds;

	private String stat;
	private int amount;

	private String area;
	private String element;
}