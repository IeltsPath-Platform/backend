package com.ieltspath.learning.domain.vo;

import com.ieltspath.learning.domain.service.StreakRules;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The streak as a learner sees it today. {@code lostStreakDays} is the length of a streak lost before today.
 * {@code frozenDays} are the days of the latest run of days a freeze covered, oldest first; empty when none was used.
 */
public record StreakView(int currentDays, int longestDays, boolean freezeAvailable, int nextFreezeCost,
                         boolean activeToday, int lostStreakDays, List<LocalDate> frozenDays) {

    /** {@code state} must already have the empty days before {@code today} applied. */
    public static StreakView of(StreakState state, LocalDate today) {
        return new StreakView(state.currentDays(), state.longestDays(), state.freezeAvailable(),
                StreakRules.freezeCost(state, today), StreakRules.activeOn(state, today), state.lostStreakDays(),
                frozenDays(state));
    }

    private static List<LocalDate> frozenDays(StreakState state) {
        if (state.frozenRunEnd() == null || state.freezeRun() <= 0) return List.of();
        return IntStream.iterate(state.freezeRun() - 1, offset -> offset >= 0, offset -> offset - 1)
                .mapToObj(offset -> state.frozenRunEnd().minusDays(offset)).toList();
    }
}
