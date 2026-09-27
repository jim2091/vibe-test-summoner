package com.jim.summoner.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonsterSourceResponse {

	private int id;

	private String nameKo;
	private String nameEn;

	private String descriptionKo;
	private String descriptionEn;

	private boolean farmable;
}