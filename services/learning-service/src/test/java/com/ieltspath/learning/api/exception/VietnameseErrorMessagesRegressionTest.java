package com.ieltspath.learning.api.exception;

import com.ieltspath.commonsecurity.error.VietnameseErrorMessages;
import com.ieltspath.learning.application.exception.InsufficientPointsException;
import com.ieltspath.learning.domain.exception.ConflictException;
import com.ieltspath.learning.domain.exception.HeartsDepletedException;
import com.ieltspath.learning.domain.exception.LearningGateException;
import com.ieltspath.learning.domain.exception.PracticeAttemptAlreadySubmittedException;
import com.ieltspath.learning.domain.exception.StudyTrackingException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VietnameseErrorMessagesRegressionTest {

    @Test
    void domainAndApplicationExceptionsUseVietnameseMessages() {
        List<RuntimeException> exceptions = List.of(
                new ConflictException(),
                new InsufficientPointsException(),
                new HeartsDepletedException(Instant.now(), 10),
                new PracticeAttemptAlreadySubmittedException(),
                new LearningGateException("REVIEW_REQUIRED", List.of()),
                new LearningGateException("TOPIC_LOCKED", List.of()),
                new LearningGateException("LESSON_LOCKED", List.of()),
                new LearningGateException("PRACTICE_REQUIRED", List.of()),
                new LearningGateException("PAYWALL", List.of())
        );

        for (RuntimeException exception : exceptions) {
            assertThat(VietnameseErrorMessages.isVietnamese(exception.getMessage()))
                    .as("Expected Vietnamese message for %s, got: %s",
                            exception.getClass().getSimpleName(), exception.getMessage())
                    .isTrue();
        }
    }

    @Test
    void handlersReplaceNonVietnameseMessages() {
        GlobalExceptionHandler global = new GlobalExceptionHandler();
        StudyDataExceptionHandler studyData = new StudyDataExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/learning/study-sessions/123");

        var forbidden = global.forbidden(new org.springframework.security.access.AccessDeniedException("Denied"), request);
        assertThat(forbidden.getBody()).isNotNull();
        assertThat(VietnameseErrorMessages.isVietnamese(forbidden.getBody().message())).isTrue();

        var study = studyData.studyTracking(new StudyTrackingException("SESSION_NOT_FOUND"), request);
        assertThat(study.getBody()).isNotNull();
        assertThat(study.getBody().details()).containsEntry("code", "SESSION_NOT_FOUND");
        assertThat(VietnameseErrorMessages.isVietnamese(study.getBody().message())).isTrue();

        var bad = studyData.badRequest(new IllegalArgumentException("Raw English error"), request);
        assertThat(bad.getBody()).isNotNull();
        assertThat(VietnameseErrorMessages.isVietnamese(bad.getBody().message())).isTrue();
    }
}
