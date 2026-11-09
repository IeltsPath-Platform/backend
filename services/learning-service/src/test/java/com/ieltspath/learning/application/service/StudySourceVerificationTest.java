package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AssessmentStudyContext;
import com.ieltspath.learning.application.port.StudySourceVerifier.Context;
import com.ieltspath.learning.application.result.StudyContextResult;
import com.ieltspath.learning.application.usecase.GetStudyContextUseCase;
import com.ieltspath.learning.domain.aggregate.PracticeSession;
import com.ieltspath.learning.domain.repository.PracticeSessionRepository;
import com.ieltspath.learning.domain.exception.StudyTrackingException;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lesson, practice and review sources are checked in Learning itself; an assessment is asked of the Assessment Service. */
class StudySourceVerificationTest {
    private final UUID user = UUID.randomUUID();
    private final UUID source = UUID.randomUUID();
    private final UUID course = UUID.randomUUID();
    private final GetStudyContextUseCase local = mock(GetStudyContextUseCase.class);
    private final AssessmentStudyContext assessment = mock(AssessmentStudyContext.class);
    private final PracticeSessionRepository practiceSessions = mock(PracticeSessionRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC);
    private final StudySourceVerification verification = new StudySourceVerification(local, assessment, practiceSessions, clock);

    @Test
    void lessonPracticeAndReviewSourcesAreCheckedLocallyForTheLearner() {
        for (String type : new String[]{"LESSON", "PRACTICE", "REVIEW"}) {
            when(local.execute(user, type, source)).thenReturn(new StudyContextResult(type, source, course));

            assertEquals(new Context(source, course), verification.verify(user, type, source));
        }
        verifyNoInteractions(assessment);
    }

    @Test
    void anAssessmentSourceIsAskedOfTheAssessmentService() {
        when(assessment.context(source)).thenReturn(new Context(source, course));

        assertEquals(new Context(source, course), verification.verify(user, "ASSESSMENT", source));
        verifyNoInteractions(local);
    }

    @Test
    void aSourceTheLearnerCannotUseIsNotAvailable() {
        for (int status : new int[]{404, 403, 409}) {
            doThrow(new LearningRequestException(status, "ANY", "refused")).when(local).execute(user, "LESSON", source);

            var thrown = assertThrows(StudyTrackingException.class, () -> verification.verify(user, "LESSON", source));
            assertEquals("SOURCE_NOT_AVAILABLE", thrown.getCode());
        }
    }

    @Test
    void practiceSessionsAreVerifiedLocallyForOwnerKindAndExpiry() {
        for (String kind : new String[]{"WRITING", "SPEAKING", "VOCABULARY"}) {
            var session = PracticeSession.start(user, kind, UUID.randomUUID(), clock.instant());
            when(practiceSessions.findOwned(session.id(), user)).thenReturn(java.util.Optional.of(session));
            assertEquals(new Context(session.id(), null), verification.verify(user, kind, session.id()));
            String otherKind = kind.equals("SPEAKING") ? "WRITING" : "SPEAKING";
            assertEquals("SOURCE_NOT_AVAILABLE", assertThrows(StudyTrackingException.class,
                    () -> verification.verify(user, otherKind, session.id())).getCode());
        }
        verifyNoInteractions(local, assessment);
    }

    @Test
    void unknownForeignAndExpiredPracticeSessionsAreNotAvailable() {
        var foreign = PracticeSession.start(UUID.randomUUID(), "WRITING", UUID.randomUUID(), clock.instant());
        var expired = PracticeSession.start(user, "WRITING", UUID.randomUUID(), clock.instant().minusSeconds(PracticeSession.VALIDITY_HOURS * 3600));
        when(practiceSessions.findOwned(source, user)).thenReturn(java.util.Optional.empty());
        when(practiceSessions.findOwned(foreign.id(), user)).thenReturn(java.util.Optional.empty());
        when(practiceSessions.findOwned(expired.id(), user)).thenReturn(java.util.Optional.of(expired));
        for (UUID id : new UUID[]{source, foreign.id(), expired.id()}) {
            assertEquals("SOURCE_NOT_AVAILABLE", assertThrows(StudyTrackingException.class,
                    () -> verification.verify(user, "WRITING", id)).getCode());
        }
        verifyNoInteractions(local, assessment);
    }

    @Test
    void anyOtherFailureMeansTheSourceCannotBeChecked() {
        doThrow(new LearningRequestException(500, "ANY", "boom")).when(local).execute(user, "LESSON", source);

        var thrown = assertThrows(StudyTrackingException.class, () -> verification.verify(user, "LESSON", source));
        assertEquals("SOURCE_UNAVAILABLE", thrown.getCode());
    }

    @Test
    void anUnknownSourceTypeIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> verification.verify(user, "VIDEO", source));
    }
}
