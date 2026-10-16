package com.ieltspath.learning.domain.vo;

import java.util.List;
import java.util.Objects;

/**
 * Thresholds and review schedule that turn a mastery score into a tier and a due date. {@code priorityBelow} and
 * {@code maintainFrom} cut the score into three tiers; a maintained point is reviewed after
 * {@code reviewIntervalsDays[success streak]} days, a consolidating one after {@code consolidateDays}.
 */
public record MasteryRules(double priorityBelow, double maintainFrom, List<Integer> reviewIntervalsDays,
                           int consolidateDays) {
    public MasteryRules {
        Objects.requireNonNull(reviewIntervalsDays, "reviewIntervalsDays");
        reviewIntervalsDays = List.copyOf(reviewIntervalsDays);
        if (!(priorityBelow > 0 && priorityBelow < maintainFrom && maintainFrom <= 1)) {
            throw new IllegalArgumentException("Thresholds must satisfy 0 < priorityBelow < maintainFrom <= 1");
        }
        if (reviewIntervalsDays.isEmpty() || reviewIntervalsDays.get(0) < 1) {
            throw new IllegalArgumentException("Review intervals must start at one day or more");
        }
        for (int index = 1; index < reviewIntervalsDays.size(); index++) {
            if (reviewIntervalsDays.get(index) <= reviewIntervalsDays.get(index - 1)) {
                throw new IllegalArgumentException("Review intervals must be strictly increasing");
            }
        }
        if (consolidateDays < 1) throw new IllegalArgumentException("consolidateDays must be at least one day");
    }

    /** The schedule shipped with the service; Admin can override every value. */
    public static MasteryRules defaults() {
        return new MasteryRules(0.60, 0.80, List.of(7, 14, 30, 45, 60), 3);
    }
}
