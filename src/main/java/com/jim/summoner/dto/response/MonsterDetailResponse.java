package com.jim.summoner.dto.response;

import java.util.List;

import com.jim.summoner.data.type.Archetype;
import com.jim.summoner.data.type.Element;
import com.jim.summoner.data.type.MonsterEntityType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonsterDetailResponse {

	private int id;

	private String nameKo;
	private String nameEn;

	private Element element;
	private Archetype archetype;
	private MonsterEntityType entityType;

	private Integer familyId;
	private List<Integer> familyMonsterIds;
	private List<MonsterFamilyMemberResponse> familyMembers;

	private int baseStars;
	private int naturalStars;

	private MonsterAwakeningResponse awakening;

	private MonsterBaseStatsResponse baseStats;
	private MonsterStatsResponse stats;

	private List<MonsterSkillResponse> skills;

	private int skillUpsToMax;

	private LeaderSkillResponse leaderSkill;

	private boolean obtainable;

	private List<MonsterSourceResponse> obtainSources;

	private MonsterFlagsResponse flags;

	private Integer transformsToId;

	private String iconUrl;
}
