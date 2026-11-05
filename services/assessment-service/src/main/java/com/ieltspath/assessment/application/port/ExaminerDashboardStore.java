package com.ieltspath.assessment.application.port;

import java.time.Instant;
import java.util.UUID;

/** The counts an examiner sees on their dashboard, from the jobs learners chose them for. QA samples are not theirs. */
public interface ExaminerDashboardStore {
    /**
     * @param reminderBefore an open job chosen at or before this moment, but not yet late, will be late soon
     * @param overdueBefore  an open job chosen at or before this moment is late
     * @param todayStart     jobs completed at or after this moment were finished today
     * @param weekStart      jobs completed at or after this moment were finished this week
     */
    Counts counts(UUID examinerId, Instant reminderBefore, Instant overdueBefore, Instant todayStart, Instant weekStart);

    record Counts(int queued, int processing, int dueSoon, int overdue, int completedToday, int completedThisWeek) {}
}
