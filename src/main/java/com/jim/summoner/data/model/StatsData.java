package com.jim.summoner.data.model;

import lombok.Data;

@Data
public class StatsData {

	private Integer hp;
	private Integer attack;
	private Integer defense;

	private Integer speed;

	private Integer critRate;
	private Integer critDamage;

	private Integer resistance;
	private Integer accuracy;
}