package com.ieltspath.assessment.application.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Both solution lists are null unless the learner passed (percent at least 70). */
public record LearnerAssessmentResult(UUID id, UUID attemptId, int resultVersion, String status, Instant completedAt,
                                      double score, double maxScore, double percent, List<Item> items,
                                      List<Solution> solutions, List<SectionSolution> sectionSolutions) {
    /** {@code correct} is null for an item that was not graded right/wrong (for example an essay). */
    public record Item(UUID attemptItemId, UUID questionVersionId, Boolean correct) {}

    /** {@code correctAnswer} is null for an item whose answer is not an objective key. */
    public record Solution(UUID attemptItemId, UUID questionVersionId, String correctAnswer, String explanation) {}

    public record SectionSolution(UUID attemptSectionId, String transcript) {}
}
