package com.ieltspath.assessment.application.port;

import com.ieltspath.assessment.domain.vo.AttemptScope;
import com.ieltspath.assessment.domain.vo.Skill;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PracticeAttemptReader {
    /** Completed (package, part) keys of a learner; an empty {@code packageIds} means every package. */
    List<ProgressKey> findProgress(UUID userId, Skill skill, AttemptScope scope, List<UUID> packageIds);
    HistoryPage findHistory(UUID userId, Skill skill, int page, int size);

    record ProgressKey(UUID packageId, Integer partNo) {}
    record HistoryPage(long totalElements, List<HistoryItem> items) {}
    record HistoryItem(UUID attemptId, UUID packageId, UUID packageVersionId, Skill skill, AttemptScope scope,
                       List<Integer> partNos, String status, Instant submittedAt, Double percent) {}
}
