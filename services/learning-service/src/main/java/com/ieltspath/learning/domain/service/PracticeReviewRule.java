package com.ieltspath.learning.domain.service;

import java.util.*;

public final class PracticeReviewRule {
    /** A knowledge point needs this many wrong answers in one set before it counts as weak; one slip is not enough. */
    public static final int MIN_WRONG_ANSWERS = 2;
    private static final double PASS = 0.70;

    public List<Need> derive(double percent, boolean countedAsEvidence, List<ItemOutcome> items,
                             Set<UUID> pendingKps, Map<UUID, Integer> availablePackages) {
        if (!countedAsEvidence || percent >= PASS) return List.of();
        Map<UUID, Counts> counts = new HashMap<>();
        for (ItemOutcome item : items) {
            for (UUID kp : item.knowledgePointIds()) {
                Counts old = counts.getOrDefault(kp, new Counts(0, 0));
                counts.put(kp, new Counts(old.correct() + (item.correct() ? 1 : 0), old.total() + 1));
            }
        }
        return counts.entrySet().stream()
                .filter(entry -> !pendingKps.contains(entry.getKey()))
                .filter(entry -> availablePackages.getOrDefault(entry.getKey(), 0) > 0)
                .filter(entry -> entry.getValue().wrong() >= MIN_WRONG_ANSWERS)
                .map(entry -> new Need(entry.getKey(),
                        (double) entry.getValue().correct() / entry.getValue().total(), entry.getValue().wrong()))
                .filter(need -> need.kpPercent() < PASS)
                .sorted(Comparator.comparing(need -> need.knowledgePointId().toString()))
                .toList();
    }

    public record ItemOutcome(Set<UUID> knowledgePointIds, boolean correct) {}
    public record Need(UUID knowledgePointId, double kpPercent, int wrong) {}
    private record Counts(int correct, int total) {
        int wrong() { return total - correct; }
    }
}
