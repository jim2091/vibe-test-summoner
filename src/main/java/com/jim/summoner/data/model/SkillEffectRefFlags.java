package com.jim.summoner.data.model;

import lombok.Data;

@Data
public class SkillEffectRefFlags {

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