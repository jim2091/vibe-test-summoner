package com.jim.summoner.data.model;

import java.util.List;

import lombok.Data;

@Data
public class SkillLocalization {

	private String name;

	private String description;

	/*
	 * raw의 level_progress_description.
	 *
	 * index 0 = Skill Level 2
	 * index 1 = Skill Level 3
	 * ...
	 */
	private List<String> levelUpDescriptions =
			List.of();
}