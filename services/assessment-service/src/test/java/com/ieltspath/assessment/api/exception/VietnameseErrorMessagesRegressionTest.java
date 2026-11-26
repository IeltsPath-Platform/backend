package com.ieltspath.assessment.api.exception;

import com.ieltspath.assessment.application.exception.UnknownErrorCodeException;
import com.ieltspath.assessment.domain.exception.AssessmentAccessDeniedException;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.exception.AttemptExpiredException;
import com.ieltspath.assessment.domain.exception.ExaminerChangeNotAllowedException;
import com.ieltspath.assessment.domain.exception.ExaminerUnavailableException;
import com.ieltspath.assessment.domain.exception.GradingCancelNotAllowedException;
import com.ieltspath.assessment.domain.exception.HumanGradingCreditConflictException;
import com.ieltspath.assessment.domain.exception.HumanGradingCreditInsufficientException;
import com.ieltspath.assessment.domain.exception.JobAlreadyClaimedException;
import com.ieltspath.assessment.domain.exception.PlacementLimitReachedException;
import com.ieltspath.assessment.domain.exception.RevisionConflictException;
import com.ieltspath.commonsecurity.error.VietnameseErrorMessages;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VietnameseErrorMessagesRegressionTest {

    @Test
    void domainAndApplicationExceptionsUseVietnameseMessages() {
        List<RuntimeException> exceptions = List.of(
                new AssessmentAccessDeniedException(),
                new AssessmentNotFoundException(UUID.randomUUID()),
                new AttemptExpiredException(),
                new HumanGradingCreditConflictException(),
                new HumanGradingCreditInsufficientException(),
                new JobAlreadyClaimedException(),
                new PlacementLimitReachedException(),
                new RevisionConflictException(1L, 2L),
                new UnknownErrorCodeException(List.of("E1")),
                new ExaminerUnavailableException(ExaminerUnavailableException.Reason.FULL),
                new ExaminerUnavailableException(ExaminerUnavailableException.Reason.NOT_ACCEPTING),
                new ExaminerUnavailableException(ExaminerUnavailableException.Reason.OVERDUE_BACKLOG),
                new ExaminerChangeNotAllowedException(ExaminerChangeNotAllowedException.Reason.NOT_OVERDUE),
                new ExaminerChangeNotAllowedException(ExaminerChangeNotAllowedException.Reason.LIMIT_REACHED),
                new ExaminerChangeNotAllowedException(ExaminerChangeNotAllowedException.Reason.SAME_EXAMINER),
                new GradingCancelNotAllowedException(GradingCancelNotAllowedException.Reason.NOT_OVERDUE),
                new GradingCancelNotAllowedException(GradingCancelNotAllowedException.Reason.CHANGES_LEFT)
        );

        for (RuntimeException exception : exceptions) {
            assertThat(VietnameseErrorMessages.isVietnamese(exception.getMessage()))
                    .as("Expected Vietnamese message for %s, got: %s",
                            exception.getClass().getSimpleName(), exception.getMessage())
                    .isTrue();
        }
    }

    @Test
    void globalExceptionHandlerReplacesNonVietnameseMessages() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/assessments/attempts/123");

        var notFound = handler.notFound(new AssessmentNotFoundException("Raw English not found"), request);
        assertThat(notFound.getBody()).isNotNull();
        assertThat(VietnameseErrorMessages.isVietnamese(notFound.getBody().message())).isTrue();

        var responseStatus = handler.responseStatus(
                new ResponseStatusException(HttpStatus.CONFLICT, "EVALUATION_NOT_PASSED"), request);
        assertThat(responseStatus.getBody()).isNotNull();
        assertThat(responseStatus.getBody().code()).isEqualTo("EVALUATION_NOT_PASSED");
        assertThat(VietnameseErrorMessages.isVietnamese(responseStatus.getBody().message())).isTrue();
    }
}
