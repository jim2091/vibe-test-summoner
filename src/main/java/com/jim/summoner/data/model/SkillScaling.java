package com.jim.summoner.data.model;

import java.util.List;

import lombok.Data;

@Data
public class SkillScaling {

	private String formula;

	private List<List<Object>> expression;

	private List<String> stats;
}