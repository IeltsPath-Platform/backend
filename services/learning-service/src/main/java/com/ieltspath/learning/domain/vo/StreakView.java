package com.ieltspath.learning.domain.vo;

import com.ieltspath.learning.domain.service.StreakRules;

import java.time.LocalDate;

/** The streak as a learner sees it today. {@code lostStreakDays} is the length of a streak lost before today. */
public record StreakView(int currentDays, int longestDays, boolean freezeAvailable, int nextFreezeCost,
                         boolean activeToday, int lostStreakDays) {

    /** {@code state} must already have the empty days before {@code today} applied. */
    public static StreakView of(StreakState state, LocalDate today) {
        return new StreakView(state.currentDays(), state.longestDays(), state.freezeAvailable(),
                StreakRules.freezeCost(state, today), StreakRules.activeOn(state, today), state.lostStreakDays());
    }
}
