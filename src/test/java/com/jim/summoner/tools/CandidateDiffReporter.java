package com.jim.summoner.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

public class CandidateDiffReporter {
    private static final Object MISSING = new Object();
    private static final int MARKDOWN_FIELD_LIMIT = 200;

    public record FieldChange(String path, boolean beforePresent, Object before, boolean afterPresent, Object after) {}
    public record Changed(int id, List<FieldChange> fields) {}
    public record DatasetDiff(boolean candidateMissing, boolean runtimeMissing, List<Integer> added,
            List<Changed> changed, List<Integer> removalCandidates, int unchangedCount) {}
    public record AssetDiff(boolean skipped, List<String> added, List<String> changed,
            List<String> runtimeOnly, int unchangedCount) {}
    public record Report(String generatedAt, Map<String, Map<String, Integer>> summary,
            Map<String, DatasetDiff> datasets, AssetDiff assets, Map<String, String> fileHashes) {}

    public static void main(String[] args) throws Exception {
        ToolFileSupport.options(args);
        generate(ToolFileSupport.root());
    }

    static Report generate(Path root) throws Exception {
        Map<String, DatasetDiff> datasets = new LinkedHashMap<>();
        Map<String, String> hashes = new TreeMap<>();
        for (String dataset : ToolFileSupport.NORMALIZED) {
            compareFile(root, "normalized/" + dataset + ".json", false, false, datasets, hashes);
        }
        for (String language : List.of("en", "ko")) {
            for (String dataset : ToolFileSupport.LOCALIZED) {
                compareFile(root, "localization/" + language + "/" + dataset + ".json",
                        true, language.equals("ko"), datasets, hashes);
            }
        }
        AssetDiff assets = compareAssets(root, hashes);
        Map<String, Map<String, Integer>> summary = new LinkedHashMap<>();
        datasets.forEach((name, diff) -> summary.put(name, Map.of(
                "ADDED", diff.added().size(), "CHANGED", diff.changed().size(),
                "REMOVAL_CANDIDATE", diff.removalCandidates().size(), "UNCHANGED", diff.unchangedCount())));
        Report report = new Report(Instant.now().toString(), summary, datasets, assets, hashes);
        ToolFileSupport.report(root, "data-source/reports/candidate-diff", report, markdown(report));
        System.out.println("Diff report generated: data-source/reports/candidate-diff.md");
        System.out.println("Removal candidates and runtime-only assets require review; no data was modified.");
        return report;
    }

    private static void compareFile(Path root, String relative, boolean localized, boolean optional,
            Map<String, DatasetDiff> datasets, Map<String, String> hashes) throws Exception {
        String candidateName = "data-source/candidate/" + relative;
        String runtimeName = "src/main/resources/game-data/" + relative;
        Path candidate = ToolFileSupport.path(root, candidateName);
        Path runtime = ToolFileSupport.path(root, runtimeName);
        boolean candidateMissing = !Files.exists(candidate);
        boolean runtimeMissing = !Files.exists(runtime);
        hashes.put(candidateName, candidateMissing ? null : ToolFileSupport.hash(candidate));
        hashes.put(runtimeName, runtimeMissing ? null : ToolFileSupport.hash(runtime));
        if (candidateMissing && optional) {
            // A missing optional KO file is not a request to remove runtime translations.
            datasets.put(relative, new DatasetDiff(true, runtimeMissing, List.of(), List.of(), List.of(), 0));
            return;
        }
        Map<Integer, Object> after = ToolFileSupport.entities(candidate, localized, false);
        Map<Integer, Object> before = ToolFileSupport.entities(runtime, localized, true);
        List<Integer> added = new ArrayList<>();
        List<Integer> removed = new ArrayList<>();
        List<Changed> changed = new ArrayList<>();
        int unchanged = 0;
        for (var entry : after.entrySet()) {
            if (!before.containsKey(entry.getKey())) added.add(entry.getKey());
            else {
                List<FieldChange> fields = new ArrayList<>();
                diff(before.get(entry.getKey()), entry.getValue(), "", fields);
                if (fields.isEmpty()) unchanged++;
                else changed.add(new Changed(entry.getKey(), fields));
            }
        }
        for (int id : before.keySet()) if (!after.containsKey(id)) removed.add(id);
        datasets.put(relative, new DatasetDiff(false, runtimeMissing, added, changed, removed, unchanged));
        // Refuse to publish a mixed snapshot when another tool changes inputs during reading.
        if (!Objects.equals(hashes.get(candidateName), ToolFileSupport.hash(candidate))
                || (!runtimeMissing && !Objects.equals(hashes.get(runtimeName), ToolFileSupport.hash(runtime)))) {
            throw new IllegalStateException("Input changed during diff: " + relative);
        }
    }

    private static void diff(Object before, Object after, String path, List<FieldChange> changes) {
        if (Objects.equals(before, after)) return;
        if ((before instanceof Map<?, ?> || before == MISSING) && (after instanceof Map<?, ?> || after == MISSING)) {
            Map<?, ?> oldMap = before instanceof Map<?, ?> map ? map : Map.of();
            Map<?, ?> newMap = after instanceof Map<?, ?> map ? map : Map.of();
            TreeSet<String> keys = new TreeSet<>();
            oldMap.keySet().forEach(key -> keys.add(String.valueOf(key)));
            newMap.keySet().forEach(key -> keys.add(String.valueOf(key)));
            if (!keys.isEmpty()) {
                for (String key : keys) diff(oldMap.containsKey(key) ? oldMap.get(key) : MISSING,
                        newMap.containsKey(key) ? newMap.get(key) : MISSING,
                        path.isEmpty() ? key : path + "." + key, changes);
                return;
            }
        }
        if ((before instanceof List<?> || before == MISSING) && (after instanceof List<?> || after == MISSING)) {
            List<?> oldList = before instanceof List<?> list ? list : List.of();
            List<?> newList = after instanceof List<?> list ? list : List.of();
            int length = Math.max(oldList.size(), newList.size());
            if (length > 0) {
                for (int index = 0; index < length; index++) diff(index < oldList.size() ? oldList.get(index) : MISSING,
                        index < newList.size() ? newList.get(index) : MISSING, path + "[" + index + "]", changes);
                return;
            }
        }
        // Expand a container/type replacement into leaves instead of one giant JSON value.
        if (before != MISSING && after != MISSING
                && (before instanceof Map<?, ?> || before instanceof List<?> || after instanceof Map<?, ?> || after instanceof List<?>)) {
            diff(before, MISSING, path, changes);
            diff(MISSING, after, path, changes);
            return;
        }
        changes.add(new FieldChange(path.isEmpty() ? "$" : path, before != MISSING,
                before == MISSING ? null : before, after != MISSING, after == MISSING ? null : after));
    }

    private static AssetDiff compareAssets(Path root, Map<String, String> hashes) throws Exception {
        if (!Files.exists(ToolFileSupport.path(root, "data-source/candidate/assets"))) {
            return new AssetDiff(true, List.of(), List.of(), List.of(), 0);
        }
        Map<String, String> after = ToolFileSupport.assets(root, "data-source/candidate/assets");
        Map<String, String> before = ToolFileSupport.assets(root, "src/main/resources/static/game-assets");
        List<String> added = new ArrayList<>(), changed = new ArrayList<>(), runtimeOnly = new ArrayList<>();
        int unchanged = 0;
        for (var entry : after.entrySet()) {
            hashes.put("data-source/candidate/assets/" + entry.getKey(), entry.getValue());
            hashes.put("src/main/resources/static/game-assets/" + entry.getKey(), before.get(entry.getKey()));
            if (!before.containsKey(entry.getKey())) added.add(entry.getKey());
            else if (!entry.getValue().equals(before.get(entry.getKey()))) changed.add(entry.getKey());
            else unchanged++;
        }
        for (var entry : before.entrySet()) {
            hashes.put("src/main/resources/static/game-assets/" + entry.getKey(), entry.getValue());
            if (!after.containsKey(entry.getKey())) runtimeOnly.add(entry.getKey());
        }
        return new AssetDiff(false, added, changed, runtimeOnly, unchanged);
    }

    private static String markdown(Report report) throws Exception {
        StringBuilder md = new StringBuilder("# Candidate Diff Report\n\nGenerated: " + report.generatedAt() + "\n\n");
        md.append("삭제 후보는 자동 삭제 대상이 아닙니다. 검토가 필요합니다.\n\n## Summary\n\n");
        md.append("| Dataset | Added | Changed | Removal Candidates | Unchanged |\n|---|---:|---:|---:|---:|\n");
        for (var entry : report.datasets().entrySet()) {
            var d = entry.getValue();
            md.append("| ").append(entry.getKey()).append(" | ").append(d.added().size()).append(" | ")
                    .append(d.changed().size()).append(" | ").append(d.removalCandidates().size()).append(" | ").append(d.unchangedCount()).append(" |\n");
        }
        for (var entry : report.datasets().entrySet()) {
            var d = entry.getValue();
            md.append("\n## ").append(entry.getKey()).append("\n\n");
            if (d.candidateMissing()) { md.append("Optional KO candidate missing — skipped; runtime will be preserved.\n"); continue; }
            if (d.runtimeMissing()) md.append("Runtime dataset missing — candidate entries are ADDED.\n\n");
            md.append("### Added\n\n").append(d.added()).append("\n\n### Changed\n\n");
            int printed = 0, total = 0;
            for (Changed entity : d.changed()) {
                total += entity.fields().size();
                if (printed >= MARKDOWN_FIELD_LIMIT) continue;
                md.append("#### ID ").append(entity.id()).append("\n\n");
                for (FieldChange field : entity.fields()) {
                    if (printed >= MARKDOWN_FIELD_LIMIT) break;
                    md.append("- `").append(ToolFileSupport.md(field.path())).append("`: ")
                            .append(value(field.beforePresent(), field.before())).append(" → ")
                            .append(value(field.afterPresent(), field.after())).append("\n");
                    printed++;
                }
            }
            if (total > printed) md.append("\n... ").append(total - printed).append(" additional field changes. See candidate-diff.json for full details.\n");
            md.append("\n### Removal Candidates — 검토 필요\n\n").append(d.removalCandidates()).append("\n");
        }
        var a = report.assets();
        md.append("\n## Assets\n\n");
        if (a.skipped()) md.append("Asset candidate not available - skipped\n");
        else {
            md.append("ADDED: ").append(a.added().size()).append("; CHANGED: ").append(a.changed().size())
                    .append("; RUNTIME_ONLY: ").append(a.runtimeOnly().size()).append("; UNCHANGED: ").append(a.unchangedCount()).append("\n\n");
            appendAssetNames(md, "ADDED", a.added());
            appendAssetNames(md, "CHANGED", a.changed());
            appendAssetNames(md, "RUNTIME_ONLY (자동 삭제하지 않음)", a.runtimeOnly());
        }
        return md.toString();
    }

    private static void appendAssetNames(StringBuilder md, String label, List<String> names) {
        md.append("### ").append(label).append("\n\n");
        names.stream().limit(200).forEach(name -> md.append("- ").append(ToolFileSupport.md(name)).append("\n"));
        if (names.size() > 200) md.append("- More entries in candidate-diff.json\n");
        md.append("\n");
    }

    private static String value(boolean present, Object value) throws Exception {
        if (!present) return "(missing)";
        String text = ToolFileSupport.JSON.writeValueAsString(value);
        if (text.length() > 300) text = text.substring(0, 300) + "… (see JSON)";
        return "`" + ToolFileSupport.md(text) + "`";
    }
}
