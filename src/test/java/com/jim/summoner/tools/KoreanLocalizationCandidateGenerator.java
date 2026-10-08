package com.jim.summoner.tools;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;

public class KoreanLocalizationCandidateGenerator {
    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("No arguments supported");
        Path root = KoreanLocalizationValidation.projectRoot();
        Path normalized = root.resolve("data-source/candidate/normalized");
        Path curation = root.resolve("data-source/curation/localization/ko");
        Path output = root.resolve("data-source/candidate/localization/ko");
        Map<String, KoreanLocalizationValidation.Result> results = new LinkedHashMap<>();
        // Validate all four sources before writing any candidate output.
        for (String dataset : KoreanLocalizationValidation.DATASETS) {
            results.put(dataset, KoreanLocalizationValidation.validate(
                    normalized.resolve(dataset + ".json"), curation.resolve(dataset + ".json"), dataset));
        }
        Files.createDirectories(output);
        JsonMapper mapper = new JsonMapper();
        System.out.println("[KOREAN LOCALIZATION]");
        for (var entry : results.entrySet()) {
            var result = entry.getValue();
            Files.writeString(output.resolve(entry.getKey() + ".json"),
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result.translations()) + "\n",
                    StandardCharsets.UTF_8);
            System.out.printf("%n%s%ntranslated = %d%ntotal      = %d%n",
                    entry.getKey(), result.translations().size(), result.total());
        }
        System.out.println("Candidate is ready for manual review. Runtime data was NOT modified.");
    }
}
