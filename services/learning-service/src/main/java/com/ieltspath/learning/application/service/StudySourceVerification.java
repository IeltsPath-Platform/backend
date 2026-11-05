package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AssessmentStudyContext;
import com.ieltspath.learning.application.port.StudySourceVerifier;
import com.ieltspath.learning.application.result.StudyContextResult;
import com.ieltspath.learning.application.usecase.GetStudyContextUseCase;
import com.ieltspath.learning.domain.exception.StudyTrackingException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Checks that the learner may log study time against a source. Lessons, practice and reviews are Learning's own and are
 * checked here; only an assessment is asked of the Assessment Service, so the two services never call each other back.
 */
@Component
public class StudySourceVerification implements StudySourceVerifier {
    private final GetStudyContextUseCase local;
    private final AssessmentStudyContext assessment;

    public StudySourceVerification(GetStudyContextUseCase local, AssessmentStudyContext assessment) {
        this.local = local;
        this.assessment = assessment;
    }

    @Override
    public Context verify(UUID userId, String sourceType, UUID sourceId) {
        return switch (sourceType) {
            case "LESSON", "PRACTICE", "REVIEW" -> own(userId, sourceType, sourceId);
            case "ASSESSMENT" -> assessment.context(sourceId);
            default -> throw new IllegalArgumentException("Unsupported study source");
        };
    }

    private Context own(UUID userId, String sourceType, UUID sourceId) {
        try {
            StudyContextResult found = local.execute(userId, sourceType, sourceId);
            return new Context(found.sourceId(), found.courseId());
        } catch (LearningRequestException refused) {
            int status = refused.getStatus();
            throw new StudyTrackingException(status == 404 || status == 403 || status == 409
                    ? "SOURCE_NOT_AVAILABLE" : "SOURCE_UNAVAILABLE");
        }
    }
}
