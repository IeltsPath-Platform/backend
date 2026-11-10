package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AssessmentStudyContext;
import com.ieltspath.learning.application.port.StudySourceVerifier;
import com.ieltspath.learning.application.result.StudyContextResult;
import com.ieltspath.learning.application.usecase.GetStudyContextUseCase;
import com.ieltspath.learning.domain.aggregate.PracticeSession;
import com.ieltspath.learning.domain.exception.StudyTrackingException;
import com.ieltspath.learning.domain.repository.PracticeSessionRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

/**
 * Checks that the learner may log study time against a source. Learning sources and server-issued practice sessions are
 * checked locally; only an assessment is asked of the Assessment Service, so the services never call each other back.
 */
@Component
public class StudySourceVerification implements StudySourceVerifier {
    private final GetStudyContextUseCase local;
    private final AssessmentStudyContext assessment;
    private final PracticeSessionRepository practiceSessions;
    private final Clock clock;

    public StudySourceVerification(GetStudyContextUseCase local, AssessmentStudyContext assessment,
            PracticeSessionRepository practiceSessions, Clock clock) {
        this.local = local;
        this.assessment = assessment;
        this.practiceSessions = practiceSessions;
        this.clock = clock;
    }

    @Override
    public Context verify(UUID userId, String sourceType, UUID sourceId) {
        return switch (sourceType) {
            case "LESSON", "PRACTICE", "REVIEW" -> own(userId, sourceType, sourceId);
            case "ASSESSMENT" -> assessment.context(sourceId);
            case "WRITING", "SPEAKING", "VOCABULARY" -> practiceSession(userId, sourceType, sourceId);
            default -> throw new IllegalArgumentException("Unsupported study source");
        };
    }

    private Context practiceSession(UUID userId, String kind, UUID id) {
        PracticeSession session = practiceSessions.findOwned(id, userId)
                .filter(found -> found.isUsableFor(userId, kind, clock.instant()))
                .orElseThrow(() -> new StudyTrackingException("SOURCE_NOT_AVAILABLE"));
        return new Context(session.id(), null);
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
