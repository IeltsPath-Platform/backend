package com.ieltspath.learning.application.port;

/**
 * Tells the grading workers that a Speaking submission was queued, so one of them starts on it at once instead of at the
 * next periodic poll. It is a hint only: the poll still finds every queued submission, so a missed wake-up costs time, not
 * a grade.
 */
public interface SpeakingGradingTrigger {
    /** Returns at once; the work happens on a worker thread. Never throws. */
    void wake();
}
