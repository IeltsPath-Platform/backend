package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.StreakState;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Streak rules, pure and day by day. A day with an answer continues the streak. An empty day is covered by a freeze when
 * one is owned and eligible, otherwise the streak breaks. Reads apply the empty days up to yesterday, so a streak lost
 * while the learner was away shows as lost without a nightly job.
 */
public final class StreakRules {
    public static final int FREEZE_PRICE = 10;
    public static final int MAX_CONSECUTIVE_FREEZES = 5;
    public static final int WEEK_XP = 100;
    public static final int MONTH_XP = 500;
    public static final int WEEK_POINTS = 10;
    private static final int MONTH_DAYS = 30;

    private StreakRules() {}

    /** Whether reaching {@code days} in a streak pays a milestone: each multiple of seven, and the thirtieth day. */
    public static boolean isMilestone(int days) {
        return days % 7 == 0 || days == MONTH_DAYS;
    }

    /** The XP a milestone pays: the thirtieth day pays more than a week. */
    public static int milestoneXp(int days) {
        return days == MONTH_DAYS ? MONTH_XP : WEEK_XP;
    }

    /** Points paid with a milestone: only the weekly ones, the thirtieth day pays XP alone. */
    public static int milestonePoints(int days) {
        return days % 7 == 0 ? WEEK_POINTS : 0;
    }

    /** The result of recording an answer on a day: the new state and the milestone values reached that day. */
    public record Recorded(StreakState state, List<Integer> milestones) {}

    /** Applies the empty days before {@code today}. Days already decided are not touched. */
    public static StreakState throughYesterday(StreakState s, LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        if (s.evaluatedThrough() == null) return s;
        StreakState state = s;
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
    public static Recorded recordAnswer(StreakState s, LocalDate today) {
        StreakState state = throughYesterday(s, today);
        if (today.equals(state.lastActive())) return new Recorded(state, List.of());
        boolean continues = state.currentDays() > 0;
        int current = continues ? state.currentDays() + 1 : 1;
        LocalDate start = continues ? state.streakStart() : today;
        StreakState next = new StreakState(current, Math.max(state.longestDays(), current), today, start, today,
                state.freezeAvailable(), state.freezeEligibleFrom(), state.frozenRunEnd(), state.freezeRun(), 0);
        List<Integer> milestones = new ArrayList<>();
        if (isMilestone(current)) milestones.add(current);
        return new Recorded(next, milestones);
    }

    /** The state as a read sees it: empty days before today applied, today not yet decided. */
    public static StreakState forRead(StreakState s, LocalDate today) {
        return throughYesterday(s, today);
    }

    /** Whether today has an answer. */
    public static boolean activeOn(StreakState s, LocalDate today) {
        return today.equals(s.lastActive());
    }

    /**
     * The price of the next freeze bought on {@code today}: 10 times the number of consecutive frozen days counting this
     * one. A run that ended before yesterday does not count.
     */
    public static int freezeCost(StreakState s, LocalDate today) {
        return FREEZE_PRICE * consecutiveWithNext(s, today);
    }

    /** Whether a further freeze would exceed the consecutive limit. */
    public static boolean overLimit(StreakState s, LocalDate today) {
        return consecutiveWithNext(s, today) > MAX_CONSECUTIVE_FREEZES;
    }

    /**
     * Records the purchase of a freeze on {@code today}. It covers the next empty day from today on: today itself if it
     * has no answer when it is judged, otherwise the first empty day after it.
     */
    public static StreakState buyFreeze(StreakState s, LocalDate today) {
        StreakState state = throughYesterday(s, today);
        return new StreakState(state.currentDays(), state.longestDays(), state.lastActive(), state.streakStart(),
                state.evaluatedThrough(), true, today, state.frozenRunEnd(), state.freezeRun(),
                state.lostStreakDays());
    }

    private static int consecutiveWithNext(StreakState s, LocalDate today) {
        boolean continuing = s.frozenRunEnd() != null && !s.frozenRunEnd().isBefore(today.minusDays(1));
        return continuing ? s.freezeRun() + 1 : 1;
    }

    private static StreakState decideEmptyDay(StreakState s, LocalDate day) {
        boolean covered = s.freezeAvailable() && s.currentDays() > 0 && !day.isBefore(s.freezeEligibleFrom());
        if (covered) {
            boolean run = s.frozenRunEnd() != null && s.frozenRunEnd().equals(day.minusDays(1));
            return new StreakState(s.currentDays(), s.longestDays(), s.lastActive(), s.streakStart(), day, false,
                    null, day, run ? s.freezeRun() + 1 : 1, s.lostStreakDays());
        }
        int lost = s.currentDays() > 0 ? s.currentDays() : s.lostStreakDays();
        return new StreakState(0, s.longestDays(), s.lastActive(), null, day, s.freezeAvailable(),
                s.freezeEligibleFrom(), s.frozenRunEnd(), s.freezeRun(), lost);
    }

    private static StreakState withEvaluated(StreakState s, LocalDate day) {
        if (s.evaluatedThrough() != null && !s.evaluatedThrough().isBefore(day)) return s;
        return new StreakState(s.currentDays(), s.longestDays(), s.lastActive(), s.streakStart(), day,
                s.freezeAvailable(), s.freezeEligibleFrom(), s.frozenRunEnd(), s.freezeRun(), s.lostStreakDays());
    }
}
