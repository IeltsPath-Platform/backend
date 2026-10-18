package com.ieltspath.assessment.application.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Both solution lists are null unless the learner passed (percent at least 70). */
public record LearnerAssessmentResult(UUID id, UUID attemptId, int resultVersion, String status, Instant completedAt,
                                      double score, double maxScore, double percent, List<Item> items,
                                      List<Solution> solutions, List<SectionSolution> sectionSolutions,
                                      Practice practice) {
    public LearnerAssessmentResult(UUID id, UUID attemptId, int resultVersion, String status, Instant completedAt,
                                   double score, double maxScore, double percent, List<Item> items,
                                   List<Solution> solutions, List<SectionSolution> sectionSolutions) {
        this(id, attemptId, resultVersion, status, completedAt, score, maxScore, percent, items, solutions,
                sectionSolutions, null);
    }

    /**
     * What a practice test result adds: when and how long, the estimated band of a full Reading or Listening test
     * (null otherwise) and where the time went, section by section.
     */
    public record Practice(com.ieltspath.assessment.domain.vo.AttemptScope scope, com.ieltspath.assessment.domain.vo.Skill skill,
                           com.ieltspath.assessment.domain.vo.AttemptMode mode, Instant startedAt, Instant submittedAt,
                           Integer timeLimitSeconds, Double band, List<SectionSummary> sections) {}

    /** {@code timeSpentSeconds} is null when the client did not report it; {@code timeLimitSeconds} when the section has none. */
    public record SectionSummary(UUID attemptSectionId, Integer partNo, String title, int questionCount,
                                 Integer timeSpentSeconds, Integer timeLimitSeconds) {}
    /** {@code correct} is null for an item that was not graded right/wrong (for example an essay). */
    public record Item(UUID attemptItemId, UUID questionVersionId, Boolean correct) {}

    /** {@code correctAnswer} is null for an item whose answer is not an objective key. */
    public record Solution(UUID attemptItemId, UUID questionVersionId, String correctAnswer, String explanation) {}

    public record SectionSolution(UUID attemptSectionId, String transcript) {}
}
