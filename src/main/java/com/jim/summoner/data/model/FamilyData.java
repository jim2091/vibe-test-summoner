package com.jim.summoner.data.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class FamilyData {

	private int id;

	private SourceIds sourceIds;

	private List<Integer> monsterIds =
			new ArrayList<>();
}