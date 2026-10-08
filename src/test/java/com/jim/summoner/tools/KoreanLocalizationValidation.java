package com.jim.summoner.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Shared, read-only validation for curation input and optional KO candidates. */
final class KoreanLocalizationValidation {
    static final List<String> DATASETS = List.of("monsters", "skills", "skill-effects", "monster-sources");
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    record Result(Map<Integer, Object> translations, int total) {}

    static Result validate(Path normalizedFile, Path localizationFile, String dataset) throws Exception {
        Object entities = readStrict(normalizedFile);
        if (!(entities instanceof List<?> list)) throw invalid(normalizedFile, "Expected entity array");
        Set<Integer> ids = new HashSet<>();
        for (Object entity : list) {
            if (!(entity instanceof Map<?, ?> map) || !(map.get("id") instanceof Integer id)
                    || id <= 0 || !ids.add(id)) {
                throw invalid(normalizedFile, "Invalid or duplicate entity internal ID");
            }
        }
        Object input = readStrict(localizationFile);
        if (!(input instanceof Map<?, ?> localizations)) throw invalid(localizationFile, "Expected localization object keyed by internal ID");
        Map<Integer, Object> translations = new TreeMap<>();
        Set<String> fields = switch (dataset) {
            case "monsters" -> Set.of("name", "familyName", "awakeningBonus");
            case "skills" -> Set.of("name", "description", "levelUpDescriptions");
            case "skill-effects", "monster-sources" -> Set.of("name", "description");
            default -> throw new IllegalArgumentException("Unknown dataset: " + dataset);
        };
        for (Map.Entry<?, ?> entry : localizations.entrySet()) {
            String key = String.valueOf(entry.getKey());
            int id;
            try {
                if (!key.matches("[1-9][0-9]*")) throw new NumberFormatException();
                id = Integer.parseInt(key);
            } catch (NumberFormatException failure) {
                throw invalid(localizationFile, "Invalid internal ID: " + key);
            }
            if (!ids.contains(id)) throw invalid(localizationFile, "Orphan internal ID: " + id);
            if (!(entry.getValue() instanceof Map<?, ?> localization)) {
                throw invalid(localizationFile, "Localization must be a non-null object: " + id);
            }
            for (Map.Entry<?, ?> field : localization.entrySet()) {
                String name = String.valueOf(field.getKey());
                Object value = field.getValue();
                if (!fields.contains(name)) throw invalid(localizationFile, "Unknown field: " + id + "." + name);
                if ("levelUpDescriptions".equals(name)) {
                    if (!(value instanceof List<?> descriptions) || descriptions.stream().anyMatch(item -> !(item instanceof String))) {
                        throw invalid(localizationFile, "Expected string array: " + id + "." + name);
                    }
                } else if (value != null && !(value instanceof String)) {
                    throw invalid(localizationFile, "Expected string or null: " + id + "." + name);
                }
            }
            if (translations.putIfAbsent(id, localization) != null) throw invalid(localizationFile, "Duplicate internal ID: " + id);
        }
        return new Result(translations, ids.size());
    }

    private static Object readStrict(Path file) throws Exception {
        // Tree parsing rejects duplicate JSON keys before Map deserialization can discard them.
        JsonNode tree = JSON.readTree(Files.readString(file));
        if (tree == null || tree.isNull()) throw invalid(file, "JSON root must not be null or empty");
        return JSON.readValue(tree.toString(), Object.class);
    }

    private static IllegalArgumentException invalid(Path file, String message) {
        return new IllegalArgumentException(file + ": " + message);
    }

    static Path projectRoot() {
        Path path = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (path != null) {
            if (Files.isRegularFile(path.resolve("pom.xml"))) return path;
            path = path.getParent();
        }
        throw new IllegalStateException("Project root not found");
    }
}
