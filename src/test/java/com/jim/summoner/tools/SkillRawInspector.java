package com.jim.summoner.tools;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

public class SkillRawInspector {

	private static final JsonMapper JSON_MAPPER =
			new JsonMapper();


	private static final List<String> DESIGN_FIELDS =
			List.of(
					"id",
					"com2us_id",
					"name",
					"description",
					"slot",
					"cooltime",
					"hits",
					"passive",
					"aoe",
					"random",
					"max_level",
					"upgrades",
					"effects",
					"multiplier_formula",
					"multiplier_formula_raw",
					"scales_with",
					"icon_filename",
					"other_skill"
			);


	private static final List<String> SIMPLE_VALUE_FIELDS =
			List.of(
					"slot",
					"cooltime",
					"hits",
					"passive",
					"aoe",
					"random",
					"max_level"
			);


	private static final List<String> COMPLEX_TARGET_FIELDS =
			List.of(
					"upgrades",
					"effects",
					"skill_effect",
					"multiplier_formula",
					"multiplier_formula_raw",
					"scales_with",
					"scaling_stats",
					"level_progress_description",
					"other_skill"
			);


	public static void main(String[] args)
			throws Exception {

		Path projectRoot =
				resolveProjectRoot();


		Path skillsFile =
				projectRoot.resolve(
						Path.of(
								"data-source",
								"raw",
								"swarfarm",
								"skills.json"
						)
				);


		Path reportDirectory =
				projectRoot.resolve(
						Path.of(
								"data-source",
								"reports"
						)
				);


		Path reportFile =
				reportDirectory.resolve(
						"skill-raw-inspection.txt"
				);


		validateRequiredFile(
				skillsFile
		);


		Files.createDirectories(
				reportDirectory
		);


		System.out.println(
				"========================================"
		);

		System.out.println(
				"Skill raw inspection start"
		);

		System.out.println(
				"========================================"
		);


		List<Map<String, Object>> skills =
				readSkills(
						skillsFile
				);


		Map<String, FieldStats> fieldStats =
				analyzeFields(
						skills
				);


		String report =
				createReport(
						skills,
						fieldStats
				);


		Files.writeString(
				reportFile,
				report,
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING
		);


		System.out.println(
				"skills = "
				+ skills.size()
		);


		System.out.println(
				"top-level fields = "
				+ fieldStats.size()
		);


		System.out.println(
				"[SAVED] "
				+ reportFile.toAbsolutePath()
		);


		System.out.println();

		System.out.println(
				"========================================"
		);

		System.out.println(
				"Skill raw inspection complete"
		);

		System.out.println(
				"========================================"
		);
	}


	// =========================================================
	// Report 생성
	// =========================================================

	private static String createReport(
			List<Map<String, Object>> skills,
			Map<String, FieldStats> fieldStats)
			throws Exception {

		StringBuilder report =
				new StringBuilder();


		appendLine(
				report,
				"========================================"
		);

		appendLine(
				report,
				"SKILL RAW INSPECTION"
		);

		appendLine(
				report,
				"========================================"
		);

		appendLine(
				report,
				""
		);


		appendLine(
				report,
				"total skills = "
				+ skills.size()
		);

		appendLine(
				report,
				"top-level field count = "
				+ fieldStats.size()
		);

		appendLine(
				report,
				""
		);


		appendDesignFieldSummary(
				report,
				skills.size(),
				fieldStats
		);


		appendAllFieldSchema(
				report,
				skills.size(),
				fieldStats
		);


		appendSimpleValueSummary(
				report,
				skills
		);


		appendComplexFieldSamples(
				report,
				skills,
				fieldStats
		);


		appendListAndObjectFieldSummary(
				report,
				fieldStats
		);


		return report.toString();
	}


	// =========================================================
	// 설계 대상 필드 존재 여부
	// =========================================================

	private static void appendDesignFieldSummary(
			StringBuilder report,
			int total,
			Map<String, FieldStats> fieldStats) {

		appendSection(
				report,
				"1. DESIGN FIELD CHECK"
		);


		for (String field :
				DESIGN_FIELDS) {

			FieldStats stats =
					fieldStats.get(
							field
					);


			if (stats == null) {

				appendLine(
						report,
						String.format(
								"%-28s present=%4d  missing=%4d  null=%4d  types=%s",
								field,
								0,
								total,
								0,
								"[]"
						)
				);

				continue;
			}


			appendLine(
					report,
					String.format(
							"%-28s present=%4d  missing=%4d  null=%4d  types=%s",
							field,
							stats.presentCount,
							total - stats.presentCount,
							stats.nullCount,
							stats.valueTypeCounts
					)
			);
		}


		appendLine(
				report,
				""
		);
	}


	// =========================================================
	// 모든 최상위 필드 구조
	// =========================================================

	private static void appendAllFieldSchema(
			StringBuilder report,
			int total,
			Map<String, FieldStats> fieldStats) {

		appendSection(
				report,
				"2. ALL TOP-LEVEL FIELD SCHEMA"
		);


		for (Map.Entry<String, FieldStats> entry :
				fieldStats.entrySet()) {

			String field =
					entry.getKey();


			FieldStats stats =
					entry.getValue();


			appendLine(
					report,
					"FIELD: "
					+ field
			);


			appendLine(
					report,
					"  present = "
					+ stats.presentCount
					+ " / "
					+ total
			);


			appendLine(
					report,
					"  missing = "
					+ (
							total
							- stats.presentCount
					)
			);


			appendLine(
					report,
					"  null = "
					+ stats.nullCount
			);


			appendLine(
					report,
					"  valueTypes = "
					+ stats.valueTypeCounts
			);


			if (!stats.listItemTypeCounts
					.isEmpty()) {

				appendLine(
						report,
						"  listItemTypes = "
						+ stats.listItemTypeCounts
				);
			}


			if (stats.minListSize
					!= null) {

				appendLine(
						report,
						"  listSize = "
						+ stats.minListSize
						+ " .. "
						+ stats.maxListSize
				);
			}


			if (!stats.objectKeyPatterns
					.isEmpty()) {

				appendLine(
						report,
						"  objectKeyPatterns:"
				);


				appendPatternCounts(
						report,
						stats.objectKeyPatterns
				);
			}


			if (!stats.listObjectKeyPatterns
					.isEmpty()) {

				appendLine(
						report,
						"  listObjectKeyPatterns:"
				);


				appendPatternCounts(
						report,
						stats.listObjectKeyPatterns
				);
			}


			appendLine(
					report,
					""
			);
		}
	}


	private static void appendPatternCounts(
			StringBuilder report,
			Map<String, Integer> patterns) {

		int printed =
				0;


		for (Map.Entry<String, Integer> entry :
				patterns.entrySet()) {

			appendLine(
					report,
					"    "
					+ entry.getKey()
					+ " -> "
					+ entry.getValue()
			);


			printed++;


			if (printed >= 20) {

				if (patterns.size() > printed) {

					appendLine(
							report,
							"    ... "
							+ (
									patterns.size()
									- printed
							)
							+ " more patterns"
					);
				}


				break;
			}
		}
	}


	// =========================================================
	// 단순 값 분포
	// =========================================================

	private static void appendSimpleValueSummary(
			StringBuilder report,
			List<Map<String, Object>> skills)
			throws Exception {

		appendSection(
				report,
				"3. SIMPLE FIELD VALUE DISTRIBUTION"
		);


		for (String field :
				SIMPLE_VALUE_FIELDS) {

			Map<String, Integer> counts =
					new TreeMap<>();


			int missing =
					0;


			int nullCount =
					0;


			for (Map<String, Object> skill :
					skills) {

				if (!skill.containsKey(
						field
				)) {

					missing++;

					continue;
				}


				Object value =
						skill.get(
								field
						);


				if (value == null) {

					nullCount++;

					continue;
				}


				String key =
						compactJson(
								value
						);


				counts.put(
						key,
						counts.getOrDefault(
								key,
								0
						) + 1
				);
			}


			appendLine(
					report,
					"FIELD: "
					+ field
			);


			appendLine(
					report,
					"  distinct = "
					+ counts.size()
			);


			appendLine(
					report,
					"  missing = "
					+ missing
					+ ", null = "
					+ nullCount
			);


			int printed =
					0;


			for (Map.Entry<String, Integer> entry :
					counts.entrySet()) {

				appendLine(
						report,
						"  "
						+ entry.getKey()
						+ " -> "
						+ entry.getValue()
				);


				printed++;


				if (printed >= 50) {

					if (counts.size()
							> printed) {

						appendLine(
								report,
								"  ... "
								+ (
										counts.size()
										- printed
								)
								+ " more values"
						);
					}


					break;
				}
			}


			appendLine(
					report,
					""
			);
		}
	}


	// =========================================================
	// 핵심 복합 필드 샘플
	// =========================================================

	private static void appendComplexFieldSamples(
			StringBuilder report,
			List<Map<String, Object>> skills,
			Map<String, FieldStats> fieldStats)
			throws Exception {

		appendSection(
				report,
				"4. TARGET COMPLEX FIELD SAMPLES"
		);


		for (String field :
				COMPLEX_TARGET_FIELDS) {

			appendLine(
					report,
					"FIELD: "
					+ field
			);


			FieldStats stats =
					fieldStats.get(
							field
					);


			if (stats == null) {

				appendLine(
						report,
						"  <field does not exist>"
				);

				appendLine(
						report,
						""
				);

				continue;
			}


			appendLine(
					report,
					"  types = "
					+ stats.valueTypeCounts
			);


			int printed =
					0;


			Set<String> duplicateGuard =
					new TreeSet<>();


			for (Map<String, Object> skill :
					skills) {

				if (!skill.containsKey(
						field
				)) {

					continue;
				}


				Object value =
						skill.get(
								field
						);


				if (value == null) {

					continue;
				}


				String serialized =
						compactJson(
								value
						);


				if (!duplicateGuard.add(
						serialized
				)) {

					continue;
				}


				appendLine(
						report,
						"  SAMPLE "
						+ (
								printed
								+ 1
						)
						+ ":"
				);


				appendLine(
						report,
						"    id = "
						+ safeText(
								skill.get(
										"id"
								)
						)
				);


				appendLine(
						report,
						"    name = "
						+ safeText(
								skill.get(
										"name"
								)
						)
				);


				appendLine(
						report,
						"    value = "
						+ truncate(
								serialized,
								1200
						)
				);


				printed++;


				if (printed >= 8) {

					break;
				}
			}


			if (printed == 0) {

				appendLine(
						report,
						"  <no non-null samples>"
				);
			}


			appendLine(
					report,
					""
			);
		}
	}


	// =========================================================
	// List / Object 필드만 다시 요약
	// =========================================================

	private static void appendListAndObjectFieldSummary(
			StringBuilder report,
			Map<String, FieldStats> fieldStats) {

		appendSection(
				report,
				"5. COMPLEX FIELD OVERVIEW"
		);


		for (Map.Entry<String, FieldStats> entry :
				fieldStats.entrySet()) {

			FieldStats stats =
					entry.getValue();


			boolean complex =
					stats.valueTypeCounts
							.containsKey(
									"LIST"
							)
					|| stats.valueTypeCounts
							.containsKey(
									"OBJECT"
							);


			if (!complex) {

				continue;
			}


			appendLine(
					report,
					entry.getKey()
					+ " -> "
					+ stats.valueTypeCounts
			);


			if (!stats.listItemTypeCounts
					.isEmpty()) {

				appendLine(
						report,
						"  itemTypes = "
						+ stats.listItemTypeCounts
				);
			}


			if (stats.minListSize
					!= null) {

				appendLine(
						report,
						"  listSize = "
						+ stats.minListSize
						+ " .. "
						+ stats.maxListSize
				);
			}
		}


		appendLine(
				report,
				""
		);
	}


	// =========================================================
	// Field 분석
	// =========================================================

	private static Map<String, FieldStats> analyzeFields(
			List<Map<String, Object>> skills) {

		Map<String, FieldStats> result =
				new TreeMap<>();


		for (Map<String, Object> skill :
				skills) {

			for (Map.Entry<String, Object> entry :
					skill.entrySet()) {

				FieldStats stats =
						result.computeIfAbsent(
								entry.getKey(),
								key -> new FieldStats()
						);


				stats.accept(
						entry.getValue()
				);
			}
		}


		return result;
	}


	private static class FieldStats {

		private int presentCount;

		private int nullCount;


		private final Map<String, Integer> valueTypeCounts =
				new TreeMap<>();


		private final Map<String, Integer> listItemTypeCounts =
				new TreeMap<>();


		private final Map<String, Integer> objectKeyPatterns =
				new TreeMap<>();


		private final Map<String, Integer> listObjectKeyPatterns =
				new TreeMap<>();


		private Integer minListSize;

		private Integer maxListSize;


		private void accept(
				Object value) {

			presentCount++;


			if (value == null) {

				nullCount++;

				increment(
						valueTypeCounts,
						"NULL"
				);

				return;
			}


			String type =
					typeName(
							value
					);


			increment(
					valueTypeCounts,
					type
			);


			if (value instanceof Map<?, ?> map) {

				increment(
						objectKeyPatterns,
						describeMapKeys(
								map
						)
				);
			}


			if (value instanceof List<?> list) {

				int size =
						list.size();


				if (minListSize == null
						|| size < minListSize) {

					minListSize =
							size;
				}


				if (maxListSize == null
						|| size > maxListSize) {

					maxListSize =
							size;
				}


				for (Object item :
						list) {

					increment(
							listItemTypeCounts,
							typeName(
									item
							)
					);


					if (item instanceof Map<?, ?> map) {

						increment(
								listObjectKeyPatterns,
								describeMapKeys(
										map
								)
						);
					}
				}
			}
		}
	}


	// =========================================================
	// 구조 표현
	// =========================================================

	private static String describeMapKeys(
			Map<?, ?> map) {

		Set<String> keys =
				new TreeSet<>();


		for (Object key :
				map.keySet()) {

			keys.add(
					String.valueOf(
							key
					)
			);
		}


		return keys.toString();
	}


	private static String typeName(
			Object value) {

		if (value == null) {

			return "NULL";
		}


		if (value instanceof Map<?, ?>) {

			return "OBJECT";
		}


		if (value instanceof List<?>) {

			return "LIST";
		}


		if (value instanceof String) {

			return "STRING";
		}


		if (value instanceof Boolean) {

			return "BOOLEAN";
		}


		if (value instanceof Integer) {

			return "INTEGER";
		}


		if (value instanceof Long) {

			return "LONG";
		}


		if (value instanceof Float
				|| value instanceof Double) {

			return "DECIMAL";
		}


		if (value instanceof Number) {

			return "NUMBER";
		}


		return value.getClass()
				.getSimpleName();
	}


	private static void increment(
			Map<String, Integer> map,
			String key) {

		map.put(
				key,
				map.getOrDefault(
						key,
						0
				) + 1
		);
	}


	// =========================================================
	// JSON / 문자열 보조
	// =========================================================

	private static String compactJson(
			Object value)
			throws Exception {

		return JSON_MAPPER
				.writeValueAsString(
						value
				);
	}


	private static String truncate(
			String value,
			int maxLength) {

		if (value == null) {

			return "";
		}


		if (value.length()
				<= maxLength) {

			return value;
		}


		return value.substring(
				0,
				maxLength
		)
		+ "... <truncated>";
	}


	private static String safeText(
			Object value) {

		return value == null
				? ""
				: String.valueOf(
						value
				);
	}


	private static void appendSection(
			StringBuilder builder,
			String title) {

		appendLine(
				builder,
				"========================================"
		);

		appendLine(
				builder,
				title
		);

		appendLine(
				builder,
				"========================================"
		);

		appendLine(
				builder,
				""
		);
	}


	private static void appendLine(
			StringBuilder builder,
			String value) {

		builder.append(
				value
		);

		builder.append(
				System.lineSeparator()
		);
	}


	// =========================================================
	// Raw 읽기
	// =========================================================

	private static List<Map<String, Object>> readSkills(
			Path file)
			throws Exception {

		try (InputStream inputStream =
				Files.newInputStream(
						file
				)) {

			return JSON_MAPPER.readValue(
					inputStream,
					new TypeReference<
							List<Map<String, Object>>>() {}
			);
		}
	}


	// =========================================================
	// 필수 파일
	// =========================================================

	private static void validateRequiredFile(
			Path file) {

		if (!Files.exists(
				file
		)) {

			throw new IllegalStateException(
					"필수 파일이 없습니다. file="
					+ file.toAbsolutePath()
			);
		}
	}


	// =========================================================
	// 프로젝트 루트
	// =========================================================

	private static Path resolveProjectRoot() {

		Path current =
				Path.of(
						System.getProperty(
								"user.dir"
						)
				)
				.toAbsolutePath()
				.normalize();


		Path cursor =
				current;


		while (cursor != null) {

			if (Files.exists(
					cursor.resolve(
							"pom.xml"
					)
			)) {

				return cursor;
			}


			cursor =
					cursor.getParent();
		}


		throw new IllegalStateException(
				"프로젝트 루트를 찾을 수 없습니다."
				+ " user.dir="
				+ current
		);
	}
}