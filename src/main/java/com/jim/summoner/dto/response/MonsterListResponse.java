package com.jim.summoner.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonsterListResponse {

	private List<MonsterListItemResponse> items;

	private PageResponse page;
}