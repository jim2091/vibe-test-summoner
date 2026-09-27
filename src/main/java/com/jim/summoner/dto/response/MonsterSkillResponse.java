package com.jim.summoner.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonsterSkillResponse {

	private int id;
	private int slot;

	private String nameKo;
	private String nameEn;

	private String descriptionKo;
	private String descriptionEn;

	private Integer cooldown;
	private int hits;

	private boolean passive;
	private boolean aoe;
	private boolean randomTarget;

	private SkillScalingResponse scaling;

	private int maxLevel;

	private List<SkillLevelUpResponse> levelUps;

	private List<SkillEffectResponse> effects;

	private Integer relatedSkillId;

	private String iconUrl;
}