package com.jim.summoner.tools;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class SwarfarmRawDownloader {

	private static final String API_BASE_URL =
			"https://swarfarm.com/api/v2/";

	private static final long PAGE_DELAY_MILLIS = 250L;

	private static final int MAX_RETRIES = 3;

	private static final JsonMapper JSON_MAPPER =
			new JsonMapper();

	private static final HttpClient HTTP_CLIENT =
			HttpClient.newBuilder()
					.connectTimeout(
							Duration.ofSeconds(15)
					)
					.followRedirects(
							HttpClient.Redirect.NORMAL
					)
					.build();


	private static final List<Dataset> DATASETS =
			List.of(
					new Dataset(
							"monsters",
							"monsters/"
					),

					new Dataset(
							"skills",
							"skills/"
					),

					new Dataset(
							"leader-skills",
							"leader-skills/"
					),

					new Dataset(
							"skill-effects",
							"skill-effects/"
					),

					new Dataset(
							"monster-sources",
							"monster-sources/"
					)
			);


	public static void main(String[] args)
			throws Exception {

		Path projectRoot =
				resolveProjectRoot();

		Path outputDirectory =
				projectRoot.resolve(
						Path.of(
								"data-source",
								"raw",
								"swarfarm"
						)
				);

		Files.createDirectories(
				outputDirectory
		);


		System.out.println(
				"========================================"
		);

		System.out.println(
				"SWARFARM raw data download start"
		);

		System.out.println(
				"output = "
				+ outputDirectory.toAbsolutePath()
		);

		System.out.println(
				"========================================"
		);


		Map<String, Integer> counts =
				new LinkedHashMap<>();


		for (Dataset dataset : DATASETS) {

			int count =
					downloadDataset(
							dataset,
							outputDirectory
					);

			counts.put(
					dataset.name(),
					count
			);
		}


		writeMeta(
				outputDirectory,
				counts
		);


		System.out.println();
		System.out.println(
				"========================================"
		);

		System.out.println(
				"SWARFARM raw data download complete"
		);

		System.out.println(
				"========================================"
		);
	}


	private static int downloadDataset(
			Dataset dataset,
			Path outputDirectory)
			throws Exception {

		System.out.println();
		System.out.println(
				"[DOWNLOAD] "
				+ dataset.name()
		);


		String nextUrl =
				API_BASE_URL
				+ dataset.endpoint();


		List<JsonNode> allResults =
				new ArrayList<>();


		int pageNumber = 1;

		int expectedCount = -1;


		while (nextUrl != null) {

			System.out.println(
					"  page "
					+ pageNumber
					+ " -> "
					+ nextUrl
			);


			String body =
					request(nextUrl);


			JsonNode root =
					JSON_MAPPER.readTree(
							body
					);


			if (expectedCount < 0) {

				JsonNode countNode =
						root.get("count");

				if (countNode == null) {

					throw new IllegalStateException(
							"SWARFARM 응답에 count가 없습니다."
							+ " dataset="
							+ dataset.name()
					);
				}

				expectedCount =
						countNode.asInt();
			}


			JsonNode results =
					root.get("results");


			if (results == null
					|| !results.isArray()) {

				throw new IllegalStateException(
						"SWARFARM 응답의 results가 배열이 아닙니다."
						+ " dataset="
						+ dataset.name()
				);
			}


			for (JsonNode result : results) {

				allResults.add(
						result
				);
			}


			JsonNode nextNode =
					root.get("next");


			if (nextNode == null
					|| nextNode.isNull()) {

				nextUrl = null;
			}
			else {

				nextUrl =
						nextNode.asString();
			}


			System.out.println(
					"    collected = "
					+ allResults.size()
					+ " / "
					+ expectedCount
			);


			pageNumber++;


			if (nextUrl != null) {

				Thread.sleep(
						PAGE_DELAY_MILLIS
				);
			}
		}


		if (expectedCount >= 0
				&& allResults.size()
						!= expectedCount) {

			throw new IllegalStateException(
					"다운로드 건수가 API count와 일치하지 않습니다."
					+ " dataset="
					+ dataset.name()
					+ ", expected="
					+ expectedCount
					+ ", actual="
					+ allResults.size()
			);
		}


		Path outputFile =
				outputDirectory.resolve(
						dataset.name()
						+ ".json"
				);


		String json =
				JSON_MAPPER
						.writerWithDefaultPrettyPrinter()
						.writeValueAsString(
								allResults
						);


		Files.writeString(
				outputFile,
				json,
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING
		);


		System.out.println(
				"[SAVED] "
				+ outputFile.toAbsolutePath()
				+ " ("
				+ allResults.size()
				+ ")"
		);


		return allResults.size();
	}


	private static String request(
			String url)
			throws Exception {

		for (int attempt = 1;
				attempt <= MAX_RETRIES;
				attempt++) {

			HttpRequest request =
					HttpRequest.newBuilder()
							.uri(
									URI.create(
											url
									)
							)
							.timeout(
									Duration.ofSeconds(30)
							)
							.header(
									"Accept",
									"application/json"
							)
							.header(
									"User-Agent",
									"summoner-data-importer/1.0"
							)
							.GET()
							.build();


			HttpResponse<String> response =
					HTTP_CLIENT.send(
							request,
							HttpResponse.BodyHandlers
									.ofString(
											StandardCharsets.UTF_8
									)
					);


			int status =
					response.statusCode();


			if (status == 200) {

				return response.body();
			}


			boolean retryable =
					status == 429
					|| status >= 500;


			if (!retryable
					|| attempt == MAX_RETRIES) {

				throw new IllegalStateException(
						"SWARFARM 요청 실패."
						+ " status="
						+ status
						+ ", url="
						+ url
				);
			}


			long waitMillis =
					1000L * attempt;


			System.out.println(
					"  요청 실패. "
					+ waitMillis
					+ "ms 후 재시도"
					+ " (status="
					+ status
					+ ")"
			);


			Thread.sleep(
					waitMillis
			);
		}


		throw new IllegalStateException(
				"SWARFARM 요청 실패: "
				+ url
		);
	}


	private static void writeMeta(
			Path outputDirectory,
			Map<String, Integer> counts)
			throws Exception {

		Map<String, Object> meta =
				new LinkedHashMap<>();


		meta.put(
				"source",
				API_BASE_URL
		);

		meta.put(
				"downloadedAt",
				Instant.now()
						.toString()
		);

		meta.put(
				"counts",
				counts
		);


		Path metaFile =
				outputDirectory.resolve(
						"meta.json"
				);


		String json =
				JSON_MAPPER
						.writerWithDefaultPrettyPrinter()
						.writeValueAsString(
								meta
						);


		Files.writeString(
				metaFile,
				json,
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING
		);


		System.out.println();
		System.out.println(
				"[SAVED] "
				+ metaFile.toAbsolutePath()
		);
	}


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
					))) {

				return cursor;
			}


			cursor =
					cursor.getParent();
		}


		throw new IllegalStateException(
				"프로젝트 루트를 찾을 수 없습니다."
				+ " pom.xml이 있는 프로젝트에서 실행해주세요."
				+ " user.dir="
				+ current
		);
	}


	private record Dataset(
			String name,
			String endpoint) {
	}
}