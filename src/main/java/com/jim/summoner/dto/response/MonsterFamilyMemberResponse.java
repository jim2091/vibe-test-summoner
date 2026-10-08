package com.jim.summoner.dto.response;

import com.jim.summoner.data.type.Archetype;
import com.jim.summoner.data.type.AwakeningStage;
import com.jim.summoner.data.type.Element;
import com.jim.summoner.data.type.MonsterEntityType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonsterFamilyMemberResponse {
    private int id;
    private String nameKo;
    private String nameEn;
    private Element element;
    private Archetype archetype;
    private int naturalStars;
    private AwakeningStage awakeningStage;
    private MonsterEntityType entityType;
    private String iconUrl;
}
