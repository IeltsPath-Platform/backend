package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.vo.ActivityCounts;

import java.time.Instant;
import java.util.List;

/** Activity and retention counts over the question event log, one database query per measure. */
public interface ActivityRepository {
    /** One row per UTC day from {@code fromDay} to {@code toDay} inclusive; both are midnights UTC. */
    List<ActivityCounts.Daily> daily(Instant fromDay, Instant toDay);

    /** Cohorts whose first-answer week starts in {@code [fromDay, toDay)}, oldest first. */
    List<ActivityCounts.Cohort> cohorts(Instant fromDay, Instant toDay);
}
