package com.jim.summoner.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PageResponse {

	private int number;
	private int size;

	private int totalElements;
	private int totalPages;
}