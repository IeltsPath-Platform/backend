package com.group01.learning.domain.service;

import java.util.*;

public final class PracticeReviewRule {
    public List<Need> derive(double percent, boolean countedAsEvidence, List<ItemOutcome> items,
                             Set<UUID> pendingKps, Map<UUID, Integer> availablePackages) {
        if (!countedAsEvidence || percent >= 0.70) return List.of();
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
                .map(entry -> new Need(entry.getKey(), (double) entry.getValue().correct() / entry.getValue().total()))
                .filter(need -> need.kpPercent() < 0.70)
                .sorted(Comparator.comparing(need -> need.knowledgePointId().toString()))
                .toList();
    }

    public record ItemOutcome(Set<UUID> knowledgePointIds, boolean correct) {}
    public record Need(UUID knowledgePointId, double kpPercent) {}
    private record Counts(int correct, int total) {}
}
