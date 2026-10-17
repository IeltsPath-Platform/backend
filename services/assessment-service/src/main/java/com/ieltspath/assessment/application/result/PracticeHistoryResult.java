package com.ieltspath.assessment.application.result;

import com.ieltspath.assessment.domain.vo.AttemptScope;
import com.ieltspath.assessment.domain.vo.Skill;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PracticeHistoryResult(long totalElements, int page, int size, List<Item> items) {
    public record Item(UUID attemptId, UUID packageId, UUID packageVersionId, Skill skill, AttemptScope scope,
                       List<Integer> partNos, String status, Instant submittedAt, Double percent) {}
}
