package com.jim.summoner.dto.request;

import java.util.List;

import com.jim.summoner.data.type.Archetype;
import com.jim.summoner.data.type.AwakeningStage;
import com.jim.summoner.data.type.Element;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class MonsterSearchRequest {

	private String keyword;

	private List<Element> elements;

	private List<
			@Min(1)
			@Max(6)
			Integer> stars;

	private List<Archetype> archetypes;

	private List<AwakeningStage> awakeningStages;

	private Boolean obtainable;

	private MonsterSortType sort = MonsterSortType.DEFAULT;

	private SortDirection direction = SortDirection.ASC;

	@Min(1)
	private int page = 1;

	@Min(1)
	@Max(100)
	private int size = 24;
}