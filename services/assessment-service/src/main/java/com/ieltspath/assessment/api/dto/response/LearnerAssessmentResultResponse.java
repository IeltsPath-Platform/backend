package com.ieltspath.assessment.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ieltspath.assessment.application.result.LearnerAssessmentResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Learner-only result. Both solution keys are absent below the pass mark. */
public record LearnerAssessmentResultResponse(UUID id, UUID attemptId, int resultVersion, String status,
                                              Instant completedAt, double score, double maxScore, double percent,
                                              List<Item> items,
                                              @JsonInclude(JsonInclude.Include.NON_NULL) List<Solution> solutions,
                                              @JsonInclude(JsonInclude.Include.NON_NULL) List<SectionSolution> sectionSolutions) {
    public record Item(UUID attemptItemId, UUID questionVersionId, Boolean correct) {}

    public record Solution(UUID attemptItemId, UUID questionVersionId, String correctAnswer, String explanation) {}

    public record SectionSolution(UUID attemptSectionId, String transcript) {}

    public static LearnerAssessmentResultResponse from(LearnerAssessmentResult r) {
        return new LearnerAssessmentResultResponse(r.id(), r.attemptId(), r.resultVersion(), r.status(),
                r.completedAt(), r.score(), r.maxScore(), r.percent(),
                r.items().stream().map(i -> new Item(i.attemptItemId(), i.questionVersionId(), i.correct())).toList(),
                r.solutions() == null ? null : r.solutions().stream()
                        .map(s -> new Solution(s.attemptItemId(), s.questionVersionId(), s.correctAnswer(),
                                s.explanation()))
                        .toList(),
                r.sectionSolutions() == null ? null : r.sectionSolutions().stream()
                        .map(s -> new SectionSolution(s.attemptSectionId(), s.transcript())).toList());
    }
}
