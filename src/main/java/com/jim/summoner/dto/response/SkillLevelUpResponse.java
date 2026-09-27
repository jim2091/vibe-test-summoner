package com.jim.summoner.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SkillLevelUpResponse {

	private int level;

	/*
	 * 원본 구조화 정보.
	 *
	 * 예:
	 * Damage +{0}%
	 * Effect Rate +{0}%
	 */
	private String effectTemplate;

	private int amount;

	/*
	 * 실제 화면 표시용 문장.
	 *
	 * 현재 KO localization이 없으므로
	 * descriptionKo는 null이 될 수 있다.
	 */
	private String descriptionKo;
	private String descriptionEn;
}