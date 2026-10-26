package com.ieltspath.library.api.dto.response;

import com.ieltspath.library.application.result.NotebookSummaryResult;
import java.time.Instant;

public record NotebookSummaryResponse(long total, ByLevel byLevel, long dueNow, long addedLast7Days,
        long reviewedToday, Instant nextDueAt) {
    public record ByLevel(long UNKNOWN, long FAMILIAR, long LEARNING, long MASTERED) {}
    public static NotebookSummaryResponse from(NotebookSummaryResult r) {
        return new NotebookSummaryResponse(r.total(), new ByLevel(r.unknown(), r.familiar(), r.learning(), r.mastered()),
                r.dueNow(), r.addedLast7Days(), r.reviewedToday(), r.nextDueAt());
    }
}
