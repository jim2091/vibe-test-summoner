package com.jim.summoner.data.model;

import lombok.Data;

@Data
public class MonsterSourceData {

	private int id;

	private SourceIds sourceIds;

	private boolean farmable;
}