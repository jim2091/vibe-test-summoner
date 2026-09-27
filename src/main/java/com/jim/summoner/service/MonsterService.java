package com.jim.summoner.service;

import com.jim.summoner.dto.request.MonsterSearchRequest;
import com.jim.summoner.dto.response.MonsterDetailResponse;
import com.jim.summoner.dto.response.MonsterListResponse;

public interface MonsterService {

	MonsterListResponse getMonsters(MonsterSearchRequest request);
	
	MonsterDetailResponse getMonster(int monsterId);
}