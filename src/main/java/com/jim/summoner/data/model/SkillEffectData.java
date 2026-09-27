package com.jim.summoner.data.model;

import com.jim.summoner.data.type.SkillEffectType;

import lombok.Data;

@Data
public class SkillEffectData {

	private int id;

	private SourceIds sourceIds;

	private SkillEffectType type;

	private SkillEffectFlags flags;

	/*
	 * icon_filename이 없는 SkillEffect도 많으므로
	 * AssetData 자체가 null일 수 있다.
	 */
	private AssetData assets;
}