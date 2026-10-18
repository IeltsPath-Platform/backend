package com.ieltspath.assessment.application.command;

import java.util.List;
import java.util.UUID;

/** {@code sectionSeconds} is how long the learner was active in each section; it may be empty. */
public record SubmitAssessmentAttemptCommand(UUID userId, UUID attemptId, List<SectionTime> sectionSeconds) {
    public record SectionTime(UUID sectionId, int seconds) {}

    public SubmitAssessmentAttemptCommand {
        sectionSeconds = sectionSeconds == null ? List.of() : List.copyOf(sectionSeconds);
    }

    public SubmitAssessmentAttemptCommand(UUID userId, UUID attemptId) {
        this(userId, attemptId, List.of());
    }
}
