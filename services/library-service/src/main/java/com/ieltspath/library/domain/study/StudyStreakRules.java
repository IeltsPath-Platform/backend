package com.ieltspath.library.domain.study;

import java.time.LocalDate;

/** Library-owned copy of the established streak rules; no milestone rewards. */
public final class StudyStreakRules {
    private StudyStreakRules() {}

    /** Applies the empty days before {@code today}. Days already decided are not touched. */
    public static StudyStreakState throughYesterday(StudyStreakState s, LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        if (s.evaluatedThrough() == null) return s;
        StudyStreakState state = s;
        LocalDate day = s.evaluatedThrough().plusDays(1);
        while (!day.isAfter(yesterday)) {
            // Nothing can change once the streak is gone and no freeze is owned: skip straight to yesterday.
            if (state.currentDays() == 0 && !state.freezeAvailable()) break;
            state = decideEmptyDay(state, day);
            day = day.plusDays(1);
        }
        return withEvaluated(state, yesterday);
    }

    /** Records an answer on {@code today}. A second answer on the same day changes nothing. */
    public static StudyStreakState recordAnswer(StudyStreakState s, LocalDate today) {
        StudyStreakState state = throughYesterday(s, today);
        if (today.equals(state.lastActive())) return state;
        boolean continues = state.currentDays() > 0;
        int current = continues ? state.currentDays() + 1 : 1;
        LocalDate start = continues ? state.streakStart() : today;
        StudyStreakState next = new StudyStreakState(current, Math.max(state.longestDays(), current), today, start, today,
                state.freezeAvailable(), state.freezeEligibleFrom(), state.frozenRunEnd(), state.freezeRun(), 0);
        return next;
    }

    /** The state as a read sees it: empty days before today applied, today not yet decided. */
    public static StudyStreakState forRead(StudyStreakState s, LocalDate today) {
        return throughYesterday(s, today);
    }

    /** Whether today has an answer. */
    public static boolean activeOn(StudyStreakState s, LocalDate today) {
        return today.equals(s.lastActive());
    }

    /**
     * The price of the next freeze bought on {@code today}: 10 times the number of consecutive frozen days counting this
     * one. A run that ended before yesterday does not count.
     */
    public static int freezeCost(StudyStreakState s, LocalDate today, int price) {
        return price * consecutiveWithNext(s, today);
    }

    /** Whether a further freeze would exceed the consecutive limit. */
    public static boolean overLimit(StudyStreakState s, LocalDate today, int maximum) {
        return consecutiveWithNext(s, today) > maximum;
    }

    /**
     * Records the purchase of a freeze on {@code today}. It covers the next empty day from today on: today itself if it
     * has no answer when it is judged, otherwise the first empty day after it.
     */
    public static StudyStreakState buyFreeze(StudyStreakState s, LocalDate today) {
        StudyStreakState state = throughYesterday(s, today);
        return new StudyStreakState(state.currentDays(), state.longestDays(), state.lastActive(), state.streakStart(),
                state.evaluatedThrough(), true, today, state.frozenRunEnd(), state.freezeRun(),
                state.lostStreakDays());
    }

    private static int consecutiveWithNext(StudyStreakState s, LocalDate today) {
        boolean continuing = s.frozenRunEnd() != null && !s.frozenRunEnd().isBefore(today.minusDays(1));
        return continuing ? s.freezeRun() + 1 : 1;
    }

    private static StudyStreakState decideEmptyDay(StudyStreakState s, LocalDate day) {
        boolean covered = s.freezeAvailable() && s.currentDays() > 0 && !day.isBefore(s.freezeEligibleFrom());
        if (covered) {
            boolean run = s.frozenRunEnd() != null && s.frozenRunEnd().equals(day.minusDays(1));
            return new StudyStreakState(s.currentDays(), s.longestDays(), s.lastActive(), s.streakStart(), day, false,
                    null, day, run ? s.freezeRun() + 1 : 1, s.lostStreakDays());
        }
        int lost = s.currentDays() > 0 ? s.currentDays() : s.lostStreakDays();
        return new StudyStreakState(0, s.longestDays(), s.lastActive(), null, day, s.freezeAvailable(),
                s.freezeEligibleFrom(), s.frozenRunEnd(), s.freezeRun(), lost);
    }

    private static StudyStreakState withEvaluated(StudyStreakState s, LocalDate day) {
        if (s.evaluatedThrough() != null && !s.evaluatedThrough().isBefore(day)) return s;
        return new StudyStreakState(s.currentDays(), s.longestDays(), s.lastActive(), s.streakStart(), day,
                s.freezeAvailable(), s.freezeEligibleFrom(), s.frozenRunEnd(), s.freezeRun(), s.lostStreakDays());
    }
}
