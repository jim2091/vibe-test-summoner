package com.jim.summoner.dto.response;

import com.jim.summoner.data.type.SkillEffectType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SkillEffectResponse {

	private int id;

	private String nameKo;
	private String nameEn;

	private String descriptionKo;
	private String descriptionEn;

	/*
	 * BUFF / DEBUFF / NEUTRAL
	 */
	private SkillEffectType type;

	/*
	 * SkillEffect Entity 자체의 is_buff 값.
	 *
	 * type과 동일하다고 가정하지 않는다.
	 */
	private boolean buff;

	/*
	 * 아래 값들은 해당 Skill에서 이 Effect가
	 * 어떻게 적용되는지를 나타내는 reference 정보.
	 */
	private Integer chance;
	private Integer quantity;

	private SkillEffectFlagsResponse flags;

	private String note;

	private String iconUrl;
}