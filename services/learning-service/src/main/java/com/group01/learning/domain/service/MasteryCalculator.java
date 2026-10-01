package com.group01.learning.domain.service;

import java.util.List;
import java.util.Objects;

// Derived from DeepTutor v1.6.9 (Apache-2.0), deeptutor/learning/mastery.py @ da856ad.
public final class MasteryCalculator {
    private static final double[] RECENCY_WEIGHTS = {0.5, 0.7, 0.85, 0.95, 1.0};

    /** Outcomes must be in chronological order, oldest first. */
    public double compute(List<Boolean> correctness) {
        Objects.requireNonNull(correctness, "correctness");
        if (correctness.isEmpty()) {
            return 0.0;
        }

        int recentCount = Math.min(correctness.size(), RECENCY_WEIGHTS.length);
        int firstOutcome = correctness.size() - recentCount;
        int firstWeight = RECENCY_WEIGHTS.length - recentCount;
        double earnedWeight = 0.0;
        double totalWeight = 0.0;
        for (int index = 0; index < recentCount; index++) {
            double weight = RECENCY_WEIGHTS[firstWeight + index];
            if (Objects.requireNonNull(correctness.get(firstOutcome + index), "correctness outcome")) {
                earnedWeight += weight;
            }
            totalWeight += weight;
        }

        double confidenceCap = switch (recentCount) {
            case 1 -> 0.5;
            case 2 -> 0.8;
            default -> 1.0;
        };
        return Math.min(earnedWeight / totalWeight, confidenceCap);
    }
}
