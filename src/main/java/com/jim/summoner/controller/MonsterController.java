package com.jim.summoner.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jim.summoner.dto.request.MonsterSearchRequest;
import com.jim.summoner.dto.response.MonsterDetailResponse;
import com.jim.summoner.dto.response.MonsterListResponse;
import com.jim.summoner.service.MonsterService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/monsters")
@RequiredArgsConstructor
public class MonsterController {

	private final MonsterService monsterService;

	@GetMapping
	public ResponseEntity<MonsterListResponse> list(
			@Valid
			@ParameterObject
			@ModelAttribute
			MonsterSearchRequest request) {

		MonsterListResponse response =
				monsterService.getMonsters(request);

		return ResponseEntity.ok(response);
	}
	
	@GetMapping("/{monsterId}")
	public ResponseEntity<MonsterDetailResponse> detail(
			@PathVariable int monsterId) {

		MonsterDetailResponse response =
				monsterService.getMonster(
						monsterId
				);

		return ResponseEntity.ok(response);
	}
}