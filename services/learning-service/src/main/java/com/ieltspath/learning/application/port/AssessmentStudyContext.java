package com.ieltspath.learning.application.port;

import com.ieltspath.learning.application.port.StudySourceVerifier.Context;

import java.util.UUID;

/** What the Assessment Service says about an assessment a learner wants to log study time against. */
public interface AssessmentStudyContext {
    /**
     * The course of the learner's assessment. Throws
     * {@link com.ieltspath.learning.domain.exception.StudyTrackingException} with {@code SOURCE_NOT_AVAILABLE} when the
     * assessment is not the learner's or is closed, and {@code SOURCE_UNAVAILABLE} when it cannot be checked.
     */
    Context context(UUID assessmentId);
}
