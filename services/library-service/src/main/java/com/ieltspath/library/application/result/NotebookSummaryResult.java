package com.ieltspath.library.application.result;

import java.time.Instant;

public record NotebookSummaryResult(long total, long unknown, long familiar, long learning, long mastered,
        long dueNow, long addedLast7Days, long reviewedToday, Instant nextDueAt) {}
