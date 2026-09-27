package com.jim.summoner.data.model;

import java.util.List;

import com.jim.summoner.data.type.Archetype;
import com.jim.summoner.data.type.Element;
import com.jim.summoner.data.type.MonsterEntityType;

import lombok.Data;

@Data
public class MonsterData {

	private int id;

	private SourceIds sourceIds;

	private Integer familyId;

	private Element element;
	private Archetype archetype;

	private MonsterEntityType entityType =
			MonsterEntityType.UNKNOWN;

	private int baseStars;
	private int naturalStars;

	private AwakeningData awakening;

	private BaseStatsData baseStats;
	private StatsData stats;

	private List<MonsterSkillRef> skills =
			List.of();

	private int skillUpsToMax;

	private Integer leaderSkillId;

	private boolean obtainable;

	private List<Integer> obtainSourceIds =
			List.of();

	private MonsterFlags flags;

	private Integer transformsToId;

	private AssetData assets;
}