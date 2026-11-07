package com.ieltspath.library.domain.study;

import java.time.LocalDate;

/**
 * A learner's streak as of {@code evaluatedThrough}: every day up to and including it has been decided (active, frozen or
 * broken). {@code frozenRunEnd} and {@code freezeRun} describe the latest run of frozen days, used for the price of the next
 * freeze. A freeze covers the first empty day on or after {@code freezeEligibleFrom}.
 */
public record StudyStreakState(int currentDays, int longestDays, LocalDate lastActive, LocalDate streakStart,
                          LocalDate evaluatedThrough, boolean freezeAvailable, LocalDate freezeEligibleFrom,
                          LocalDate frozenRunEnd, int freezeRun, int lostStreakDays) {

    public static StudyStreakState empty() {
        return new StudyStreakState(0, 0, null, null, null, false, null, null, 0, 0);
    }
}
