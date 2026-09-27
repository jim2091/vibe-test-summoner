package com.jim.summoner.dto.response;

import com.jim.summoner.data.type.Archetype;
import com.jim.summoner.data.type.AwakeningStage;
import com.jim.summoner.data.type.Element;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonsterListItemResponse {

	private int id;

	private String nameKo;
	private String nameEn;

	private Element element;
	private Archetype archetype;

	private int naturalStars;

	private AwakeningStage awakeningStage;

	private String iconUrl;
}