package com.ieltspath.learning.domain.vo;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Raw activity counts from the event log, before any ratio is taken. A learner is active on a day when they answered at
 * least one question that day, in UTC. {@code dau}, {@code wau} and {@code mau} count the learners active on that day,
 * in the week ending that day and in the 30 days ending that day.
 */
public final class ActivityCounts {
    private ActivityCounts() {}

    public record Daily(LocalDate day, long dau, long wau, long mau) {}

    /**
     * A cohort is the learners whose first answer fell in the week starting {@code weekStart}. {@code weekN} counts those
     * who answered in the week {@code N} weeks after that, for N = 1, 2, 4 and 8.
     */
    public record Cohort(Instant weekStart, long size, long week1, long week2, long week4, long week8) {}
}
