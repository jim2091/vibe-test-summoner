package com.jim.summoner.tools;

import java.util.ArrayList;
import java.util.List;

public class SwarfarmDataPipelineRunner {
    @FunctionalInterface
    private interface Action { void run() throws Exception; }
    private record Step(String name, Action action) {}

    public static void main(String[] args) throws Exception {
        boolean skipDownload = false;
        for (String arg : args) {
            if (!"--skip-download".equals(arg)) {
                throw new IllegalArgumentException("Unknown argument: " + arg + ". Usage: [--skip-download]");
            }
            skipDownload = true;
        }
        List<Step> steps = new ArrayList<>();
        if (!skipDownload) steps.add(new Step("Download raw data", () -> SwarfarmRawDownloader.main(new String[0])));
        steps.add(new Step("Generate stable ID maps", () -> IdMapGenerator.main(new String[0])));
        steps.add(new Step("Generate Monster and Family candidate", () -> MonsterCandidateGenerator.main(new String[0])));
        steps.add(new Step("Generate Skill candidate", () -> SkillCandidateGenerator.main(new String[0])));
        steps.add(new Step("Generate SkillEffect candidate", () -> SkillEffectCandidateGenerator.main(new String[0])));
        steps.add(new Step("Generate LeaderSkill candidate", () -> LeaderSkillCandidateGenerator.main(new String[0])));
        steps.add(new Step("Generate MonsterSource candidate", () -> MonsterSourceCandidateGenerator.main(new String[0])));
        steps.add(new Step("Generate Korean localization candidate", () -> KoreanLocalizationCandidateGenerator.main(new String[0])));
        steps.add(new Step("Validate candidate", () -> CandidateIntegrityValidator.main(new String[0])));
        steps.add(new Step("Generate candidate diff report", () -> CandidateDiffReporter.main(new String[0])));

        System.out.println("SWARFARM data pipeline start");
        if (skipDownload) System.out.println("[SKIP] Download raw data; using existing raw files.");
        for (int index = 0; index < steps.size(); index++) {
            Step step = steps.get(index);
            System.out.printf("%n[%d/%d] %s%n[START]%n", index + 1, steps.size(), step.name());
            try {
                step.action().run();
                System.out.println("[DONE] " + step.name());
            } catch (Exception | Error failure) {
                System.err.println("[FAILED] " + step.name() + "\nPipeline aborted.");
                throw failure;
            }
        }
        System.out.println("\nSWARFARM data pipeline complete");
        System.out.println("Candidate validation passed. Diff report generated.");
        System.out.println("Candidate is ready for manual review.");
        System.out.println("Runtime data was NOT modified.");
    }
}
