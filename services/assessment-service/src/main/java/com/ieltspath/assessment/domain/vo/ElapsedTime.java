package com.ieltspath.assessment.domain.vo;

/**
 * Time a learner spent on one item, reported by the client as a running total. The server keeps it within the
 * range [0, two hours], so a stalled tab cannot inflate the figure that reports rely on.
 */
public final class ElapsedTime {
    public static final long MAX_MILLIS = 2L * 60 * 60 * 1000;

    private ElapsedTime() {}

    public static long clamp(long millis) {
        return Math.max(0L, Math.min(millis, MAX_MILLIS));
    }
}
