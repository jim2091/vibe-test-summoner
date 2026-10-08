package com.jim.summoner.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

public class CandidatePromotionTool {
    public record CopyItem(String group, String source, String target, String backupRelative,
            String action, String candidateHash, String runtimeHash) {}

    public static void main(String[] args) throws Exception {
        var options = ToolFileSupport.options(args, "--apply", "--include-assets");
        boolean apply = options.contains("--apply");
        boolean includeAssets = options.contains("--include-assets");
        Path root = ToolFileSupport.root();
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC).format(Instant.now())
                + "-" + UUID.randomUUID().toString().substring(0, 8);
        String reportStem = "data-source/reports/promotion/promotion-report-" + stamp;
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("generatedAt", Instant.now().toString());
        report.put("mode", apply ? "APPLY" : "DRY_RUN");
        report.put("includeAssets", includeAssets);
        report.put("validation", "NOT_RUN");
        report.put("diffReport", null);
        report.put("backupDirectory", null);
        report.put("rollbackPerformed", false);
        report.put("status", "STARTED");
        List<CopyItem> plan = new ArrayList<>();
        List<CopyItem> touched = new ArrayList<>();
        List<String> completed = new ArrayList<>();
        List<String> rollbackErrors = new ArrayList<>();
        report.put("files", plan);
        report.put("summary", summarize(plan, 0));
        report.put("completedBeforeRollback", completed);
        report.put("rollbackErrors", rollbackErrors);
        String backupDirectory = "data-source/backups/" + stamp;
        System.out.println("Candidate promotion start; mode=" + report.get("mode") + "; include-assets=" + includeAssets);
        try {
            Map<String, String> initial = snapshot(root);
            if (includeAssets && !Files.isDirectory(ToolFileSupport.path(root, "data-source/candidate/assets"))) {
                throw new IOException("--include-assets requires candidate/assets; run the separate downloader first.");
            }
            report.put("validation", "RUNNING");
            System.out.println("[VALIDATE]");
            CandidateIntegrityValidator.main(new String[0]);
            report.put("validation", "PASSED");
            System.out.println("PASSED\n[DIFF]");
            CandidateDiffReporter.Report diff = CandidateDiffReporter.generate(root);
            report.put("diffReport", "data-source/reports/candidate-diff.json");
            if (!initial.equals(snapshot(root))) throw new IOException("Candidate/runtime changed during validation or diff. Retry without concurrent tools.");
            buildPlan(root, diff, includeAssets, plan);
            report.put("summary", summarize(plan, diff.assets().runtimeOnly().size()));
            report.put("assetsStatus", includeAssets ? "INCLUDED" : "NOT_INCLUDED");
            List<String> removals = diff.datasets().entrySet().stream()
                    .filter(entry -> !entry.getValue().removalCandidates().isEmpty()).map(Map.Entry::getKey).toList();
            report.put("removalCandidatesRequireReview", removals);
            if (!removals.isEmpty()) {
                report.put("applyBlockedReason", "Runtime-only entities/translations must be reconciled in candidate before promotion; automatic deletion is prohibited.");
                System.out.println("APPLY BLOCKED: removal candidates in " + removals);
                if (apply) throw new IOException(String.valueOf(report.get("applyBlockedReason")));
            }
            List<CopyItem> changes = plan.stream().filter(item -> !item.action().equals("UNCHANGED")).toList();
            for (CopyItem item : plan) System.out.println(item.action() + " " + item.target());
            if (changes.isEmpty()) System.out.println("No changes to promote. Runtime was not modified.");
            if (apply && !changes.isEmpty()) {
                if (!initial.equals(snapshot(root))) throw new IOException("Inputs changed before backup");
                report.put("backupDirectory", backupDirectory);
                System.out.println("[BACKUP] " + backupDirectory);
                Files.createDirectories(ToolFileSupport.path(root, backupDirectory));
                // Finish every backup before replacing any runtime file.
                for (CopyItem item : changes) {
                    assertHash(root, item.source(), item.candidateHash());
                    assertHash(root, item.target(), item.runtimeHash());
                    if (item.runtimeHash() != null) {
                        Path backup = ToolFileSupport.path(root, backupDirectory + "/" + item.backupRelative());
                        ToolFileSupport.copy(root, ToolFileSupport.path(root, item.target()), backup);
                        if (!item.runtimeHash().equals(ToolFileSupport.hash(backup))) throw new IOException("Backup mismatch: " + item.target());
                    }
                }
                if (!initial.equals(snapshot(root))) throw new IOException("Inputs changed while backing up");
                for (CopyItem item : changes) {
                    assertHash(root, item.source(), item.candidateHash());
                    assertHash(root, item.target(), item.runtimeHash());
                    // Include the in-flight file: a non-atomic replacement fallback may fail after mutation.
                    System.out.println("[PROMOTE " + item.group() + "] " + item.target());
                    touched.add(item);
                    ToolFileSupport.copy(root, ToolFileSupport.path(root, item.source()), ToolFileSupport.path(root, item.target()));
                    assertHash(root, item.target(), item.candidateHash());
                    completed.add(item.target());
                }
                // Detect candidate edits during apply, including already-copied files.
                Map<String, String> finalInputs = snapshot(root);
                Map<String, String> expectedCandidate = new TreeMap<>(initial);
                Map<String, String> actualCandidate = new TreeMap<>(finalInputs);
                expectedCandidate.keySet().removeIf(key -> !key.startsWith("data-source/candidate/"));
                actualCandidate.keySet().removeIf(key -> !key.startsWith("data-source/candidate/"));
                if (!expectedCandidate.equals(actualCandidate)) throw new IOException("Candidate changed during apply");
            }
            report.put("status", apply ? "SUCCESS" : "DRY_RUN_COMPLETE");
            writeReport(root, reportStem, report);
            if (!apply) System.out.println("DRY RUN ONLY\nNo runtime files were modified.\nUse --apply to promote reviewed candidate.");
            else System.out.println("Candidate promotion complete");
            System.out.println("Report: " + reportStem + ".md");
        } catch (Exception | Error failure) {
            if ("RUNNING".equals(report.get("validation"))) report.put("validation", "FAILED");
            report.put("status", "FAILED");
            report.put("error", failure.toString());
            if (!touched.isEmpty()) {
                report.put("rollbackPerformed", true);
                for (int index = touched.size() - 1; index >= 0; index--) {
                    CopyItem item = touched.get(index);
                    try {
                        Path target = ToolFileSupport.path(root, item.target());
                        if (item.runtimeHash() == null) Files.deleteIfExists(target);
                        else {
                            Path backup = ToolFileSupport.path(root, backupDirectory + "/" + item.backupRelative());
                            if (!item.runtimeHash().equals(ToolFileSupport.hash(backup))) throw new IOException("Backup changed: " + backup);
                            ToolFileSupport.copy(root, backup, target);
                            assertHash(root, item.target(), item.runtimeHash());
                        }
                    } catch (Exception | Error rollbackFailure) {
                        rollbackErrors.add(item.target() + ": " + rollbackFailure);
                        failure.addSuppressed(rollbackFailure);
                        System.err.println("ROLLBACK FAILED: " + item.target() + ": " + rollbackFailure);
                    }
                }
            }
            report.put("rollbackComplete", !touched.isEmpty() && rollbackErrors.isEmpty());
            try { writeReport(root, reportStem, report); }
            catch (Exception | Error reportFailure) {
                failure.addSuppressed(reportFailure);
                System.err.println("Promotion report write failed: " + reportFailure);
            }
            System.err.println("Promotion failed: " + failure);
            throw failure;
        }
    }

    private static void buildPlan(Path root, CandidateDiffReporter.Report diff, boolean includeAssets, List<CopyItem> plan) throws Exception {
        for (var entry : diff.datasets().entrySet()) {
            var dataset = entry.getValue();
            if (dataset.candidateMissing()) continue;
            String relative = entry.getKey();
            String source = "data-source/candidate/" + relative;
            String target = "src/main/resources/game-data/" + relative;
            boolean changed = dataset.runtimeMissing() || !dataset.added().isEmpty() || !dataset.changed().isEmpty() || !dataset.removalCandidates().isEmpty();
            String oldHash = diff.fileHashes().get(target);
            plan.add(new CopyItem(relative.startsWith("normalized/") ? "data" : "localization", source, target,
                    "game-data/" + relative, !changed ? "UNCHANGED" : oldHash == null ? "CREATED" : "REPLACED",
                    diff.fileHashes().get(source), oldHash));
        }
        if (!includeAssets) return;
        var assets = ToolFileSupport.assets(root, "data-source/candidate/assets");
        for (var entry : assets.entrySet()) {
            String source = "data-source/candidate/assets/" + entry.getKey();
            String target = "src/main/resources/static/game-assets/" + entry.getKey();
            if (!ToolFileSupport.png(ToolFileSupport.path(root, source))) throw new IOException("Invalid candidate PNG: " + source);
            String newHash = diff.fileHashes().get(source);
            if (!entry.getValue().equals(newHash)) throw new IOException("Asset changed after diff: " + source);
            String oldHash = diff.fileHashes().get(target);
            plan.add(new CopyItem("assets", source, target, "game-assets/" + entry.getKey(),
                    oldHash == null ? "CREATED" : oldHash.equals(newHash) ? "UNCHANGED" : "REPLACED", newHash, oldHash));
        }
    }

    private static Map<String, Map<String, Integer>> summarize(List<CopyItem> plan, int runtimeOnly) {
        Map<String, Map<String, Integer>> summary = new LinkedHashMap<>();
        for (String group : List.of("data", "localization", "assets")) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String action : List.of("CREATED", "REPLACED", "UNCHANGED")) {
                counts.put(action, (int) plan.stream().filter(item -> item.group().equals(group) && item.action().equals(action)).count());
            }
            if (group.equals("assets")) counts.put("RUNTIME_ONLY", runtimeOnly);
            summary.put(group, counts);
        }
        return summary;
    }

    private static Map<String, String> snapshot(Path root) throws Exception {
        Map<String, String> hashes = new TreeMap<>();
        List<String> json = new ArrayList<>();
        for (String name : ToolFileSupport.NORMALIZED) json.add("normalized/" + name + ".json");
        for (String language : List.of("en", "ko")) {
            for (String name : ToolFileSupport.LOCALIZED) json.add("localization/" + language + "/" + name + ".json");
        }
        for (String relative : json) {
            for (String base : List.of("data-source/candidate/", "src/main/resources/game-data/")) {
                Path file = ToolFileSupport.path(root, base + relative);
                hashes.put(base + relative, Files.exists(file) ? ToolFileSupport.hash(file) : null);
            }
        }
        for (String base : List.of("data-source/candidate/assets", "src/main/resources/static/game-assets")) {
            for (var asset : ToolFileSupport.assets(root, base).entrySet()) hashes.put(base + "/" + asset.getKey(), asset.getValue());
        }
        return hashes;
    }

    private static void assertHash(Path root, String relative, String expected) throws Exception {
        Path file = ToolFileSupport.path(root, relative);
        String actual = Files.exists(file) ? ToolFileSupport.hash(file) : null;
        if (!Objects.equals(expected, actual)) throw new IOException("File changed since review: " + relative);
    }

    private static void writeReport(Path root, String stem, Map<String, Object> report) throws Exception {
        StringBuilder md = new StringBuilder("# Candidate Promotion Report\n\n");
        for (String field : List.of("generatedAt", "mode", "includeAssets", "validation", "diffReport", "backupDirectory",
                "status", "error", "rollbackPerformed", "rollbackComplete", "applyBlockedReason", "assetsStatus")) {
            if (report.get(field) != null) md.append("- ").append(field).append(": ").append(ToolFileSupport.md(report.get(field))).append("\n");
        }
        md.append("\n## Planned file counts\n\n| Group | Created | Replaced | Unchanged | Runtime only |\n|---|---:|---:|---:|---:|\n");
        if (report.get("summary") instanceof Map<?, ?> summary) {
            for (var entry : summary.entrySet()) {
                Map<?, ?> counts = (Map<?, ?>) entry.getValue();
                md.append("| ").append(entry.getKey()).append(" | ").append(counts.get("CREATED"))
                        .append(" | ").append(counts.get("REPLACED")).append(" | ").append(counts.get("UNCHANGED"))
                        .append(" | ").append(counts.containsKey("RUNTIME_ONLY") ? counts.get("RUNTIME_ONLY") : "—").append(" |\n");
            }
        }
        md.append("\n");
        md.append("Counts describe the plan; completedBeforeRollback and rollbackErrors in JSON record actual progress.\n\n");
        md.append("## Rollback errors\n\n").append(ToolFileSupport.md(report.get("rollbackErrors"))).append("\n\n");
        md.append("See the JSON report for the complete file plan. Runtime-only assets are never deleted.\n");
        ToolFileSupport.report(root, stem, report, md.toString());
    }
}
