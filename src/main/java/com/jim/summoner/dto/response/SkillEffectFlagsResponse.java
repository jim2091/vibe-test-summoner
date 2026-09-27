package com.jim.summoner.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SkillEffectFlagsResponse {

	private boolean aoe;
	private boolean singleTarget;
	private boolean selfEffect;

	private boolean onCrit;
	private boolean onDeath;

	private boolean random;
	private boolean allTargets;

	private boolean selfHp;
	private boolean targetHp;

	private boolean damage;
}