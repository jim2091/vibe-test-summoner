package com.jim.summoner.tools;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SwarfarmAssetCandidateDownloader {
    private static final String MONSTER_IMAGE_BASE_URL = "https://swarfarm.com/static/herders/images/monsters/";
    private static final String SKILL_IMAGE_BASE_URL = "https://swarfarm.com/static/herders/images/skills/";
    private static final String EFFECT_IMAGE_BASE_URL = "https://swarfarm.com/static/herders/images/buffs/";
    private static final int MAX_ATTEMPTS = 3;
    private static final long REQUEST_DELAY_MILLIS = 250;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL).build();
    private record Dataset(String name, String directory, String prefix, String field, String baseUrl, boolean optionalIcon) {}
    private static final List<Dataset> DATASETS = List.of(
            new Dataset("monsters", "monsters", "monster-", "image_filename", MONSTER_IMAGE_BASE_URL, false),
            new Dataset("skills", "skills", "skill-", "icon_filename", SKILL_IMAGE_BASE_URL, false),
            new Dataset("skill-effects", "effects", "effect-", "icon_filename", EFFECT_IMAGE_BASE_URL, true));
    private record Job(String entityType, int internalId, long swarfarmId, String filename, String url, Path target) {}
    public record Failure(String entityType, Integer internalId, long swarfarmId, String sourceFilename, String sourceUrl, String reason) {}
    public record DownloadReport(String generatedAt, boolean force, Map<String, Map<String, Integer>> datasets,
            int uniqueSourceUrls, int requestedSourceUrls, List<Failure> failedItems) {}

    public static void main(String[] args) throws Exception {
        boolean force = ToolFileSupport.options(args, "--force").contains("--force");
        Path root = ToolFileSupport.root();
        Map<String, Map<String, Integer>> counts = new LinkedHashMap<>();
        Map<String, List<Job>> grouped = new LinkedHashMap<>();
        Set<String> sourceUrls = new HashSet<>();
        List<Failure> failures = new ArrayList<>();
        System.out.println("SWARFARM asset candidate download start");
        for (Dataset dataset : DATASETS) prepare(root, dataset, force, counts, grouped, sourceUrls, failures);
        int requested = 0;
        for (var group : grouped.entrySet()) {
            requested++;
            byte[] bytes;
            try { bytes = download(group.getKey()); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw interrupted; }
            catch (Exception failure) {
                for (Job job : group.getValue()) failed(job, failure, counts, failures);
                continue;
            }
            for (Job job : group.getValue()) {
                try {
                    ToolFileSupport.write(root, job.target(), bytes);
                    increment(counts.get(job.entityType()), "downloaded");
                } catch (Exception failure) { failed(job, failure, counts, failures); }
            }
        }
        DownloadReport report = new DownloadReport(Instant.now().toString(), force, counts, sourceUrls.size(), requested, failures);
        StringBuilder md = new StringBuilder("# Asset download report\n\nGenerated: " + report.generatedAt() + "\n\n");
        md.append("| Dataset | Total | Source available | No source icon | Downloaded | Skipped existing | Failed |\n|---|---:|---:|---:|---:|---:|---:|\n");
        for (var entry : counts.entrySet()) {
            var c = entry.getValue();
            System.out.println("[" + entry.getKey() + "] " + c);
            md.append("| ").append(entry.getKey());
            for (String key : List.of("total", "sourceAvailable", "noSourceIcon", "downloaded", "skippedExisting", "failed")) md.append(" | ").append(c.get(key));
            md.append(" |\n");
        }
        md.append("\nUnique source URLs: ").append(sourceUrls.size()).append("\n\nRequested source URLs: ").append(requested).append("\n\n## Failed items\n\n");
        for (Failure failure : failures) md.append("- ").append(ToolFileSupport.md(failure)).append("\n");
        md.append("\nNO_SOURCE_ICON is normal for skill effects. Runtime assets were NOT modified.\n");
        ToolFileSupport.report(root, "data-source/reports/assets/asset-download-report", report, md.toString());
        System.out.println("report: data-source/reports/assets/asset-download-report.md");
        System.out.println("Asset candidate download complete. Runtime assets were NOT modified.");
        if (!failures.isEmpty()) throw new IOException("Asset download completed with " + failures.size() + " failed items; see report.");
    }

    private static void prepare(Path root, Dataset dataset, boolean force, Map<String, Map<String, Integer>> counts,
            Map<String, List<Job>> grouped, Set<String> urls, List<Failure> failures) throws Exception {
        Map<String, Integer> count = new LinkedHashMap<>();
        for (String key : List.of("total", "sourceAvailable", "noSourceIcon", "downloaded", "skippedExisting", "failed")) count.put(key, 0);
        counts.put(dataset.name(), count);
        Path output = ToolFileSupport.path(root, "data-source/candidate/assets/" + dataset.directory());
        Files.createDirectories(output);
        var raw = ToolFileSupport.array(ToolFileSupport.read(ToolFileSupport.path(root, "data-source/raw/swarfarm/" + dataset.name() + ".json")), dataset.name());
        var mapRoot = ToolFileSupport.object(ToolFileSupport.read(ToolFileSupport.path(root, "data-source/id-map/" + dataset.name() + ".json")), "id-map");
        var bySwarfarmId = ToolFileSupport.object(mapRoot.get("bySwarfarmId"), dataset.name() + ".bySwarfarmId");
        for (var entry : bySwarfarmId.entrySet()) {
            if (!String.valueOf(entry.getKey()).matches("[1-9][0-9]*")) throw new IOException("Invalid SWARFARM map key: " + entry.getKey());
            ToolFileSupport.id(entry.getValue(), dataset.name() + " mapping " + entry.getKey());
        }
        var candidates = ToolFileSupport.entities(ToolFileSupport.path(root, "data-source/candidate/normalized/" + dataset.name() + ".json"), false, false);
        Set<Long> seen = new HashSet<>();
        Map<Integer, Long> targetSources = new LinkedHashMap<>();
        for (Object value : raw) {
            var entity = ToolFileSupport.object(value, dataset.name());
            Object rawId = entity.get("id");
            if (!(rawId instanceof Integer || rawId instanceof Long) || ((Number) rawId).longValue() <= 0) throw new IOException("Invalid raw ID: " + rawId);
            long swarfarmId = ((Number) rawId).longValue();
            if (!seen.add(swarfarmId)) throw new IOException("Duplicate raw SWARFARM ID: " + swarfarmId);
            increment(count, "total");
            Object source = entity.get(dataset.field());
            String filename = source instanceof String text ? text.trim() : null;
            String url = filename != null && filename.matches("(?i)[a-z0-9][a-z0-9_.-]*\\.png")
                    ? dataset.baseUrl() + filename : null;
            Integer internalId = null;
            Object mapping = bySwarfarmId.get(String.valueOf(swarfarmId));
            if (mapping instanceof Integer mapped) {
                Long previousSource = targetSources.putIfAbsent(mapped, swarfarmId);
                if (previousSource != null) throw new IOException("Multiple current raw entities map to internal ID " + mapped);
            }
            if (filename != null && !filename.isBlank()) increment(count, "sourceAvailable");
            try {
                Object mappedId = bySwarfarmId.get(String.valueOf(swarfarmId));
                internalId = ToolFileSupport.id(mappedId, dataset.name() + " source " + swarfarmId);
                if (source != null && !(source instanceof String)) throw new IOException("Source filename must be a string or null");
                if (filename == null || filename.isBlank()) {
                    if (!dataset.optionalIcon()) throw new IOException("Missing source icon");
                    increment(count, "noSourceIcon");
                    continue;
                }
                if (!filename.matches("(?i)[a-z0-9][a-z0-9_.-]*\\.png")) throw new IOException("Unexpected PNG filename: " + filename);
                url = dataset.baseUrl() + filename;
                urls.add(url);
                var candidate = ToolFileSupport.object(candidates.get(internalId), "Candidate entity " + internalId);
                var assets = ToolFileSupport.object(candidate.get("assets"), "Candidate assets " + internalId);
                String iconKey = dataset.prefix() + internalId;
                if (!iconKey.equals(assets.get("iconKey"))) throw new IOException("Candidate iconKey does not match stable internal ID: " + internalId);
                Path target = ToolFileSupport.path(root, "data-source/candidate/assets/" + dataset.directory() + "/" + iconKey + ".png");
                if (!force && ToolFileSupport.png(target)) { increment(count, "skippedExisting"); continue; }
                grouped.computeIfAbsent(url, ignored -> new ArrayList<>()).add(new Job(dataset.name(), internalId, swarfarmId, filename, url, target));
            } catch (Exception failure) {
                increment(count, "failed");
                failures.add(new Failure(dataset.name(), internalId, swarfarmId, filename, url, failure.getMessage()));
            }
        }
    }

    private static byte[] download(String url) throws Exception {
        IOException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            Thread.sleep(REQUEST_DELAY_MILLIS * attempt);
            HttpResponse<byte[]> response;
            try {
                response = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                        .header("Accept", "image/png").GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            } catch (IOException failure) { last = failure; continue; }
            int status = response.statusCode();
            if (status == 429 || status >= 500) { last = new IOException("HTTP " + status); continue; }
            if (status != 200) throw new IOException("HTTP " + status + " for " + url);
            byte[] body = response.body();
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (!ToolFileSupport.png(body)) throw new IOException("Invalid PNG body; Content-Type=" + contentType);
            return body;
        }
        throw new IOException("Download failed after " + MAX_ATTEMPTS + " attempts: " + url, last);
    }

    private static void increment(Map<String, Integer> counts, String key) { counts.put(key, counts.get(key) + 1); }
    private static void failed(Job job, Exception failure, Map<String, Map<String, Integer>> counts, List<Failure> failures) {
        increment(counts.get(job.entityType()), "failed");
        failures.add(new Failure(job.entityType(), job.internalId(), job.swarfarmId(), job.filename(), job.url(), failure.getMessage()));
    }
}
