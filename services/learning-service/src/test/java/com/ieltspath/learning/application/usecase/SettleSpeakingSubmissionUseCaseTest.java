package com.ieltspath.learning.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.exception.InsufficientPointsException;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.service.SpeakingPathUpdater;
import com.ieltspath.learning.application.service.SpeakingSettingsFixture;
import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettleSpeakingSubmissionUseCaseTest {
    private static final UUID USER = UUID.randomUUID();
    private static final UUID SUBMISSION = UUID.randomUUID();
    private static final UUID REQUEST = UUID.randomUUID();
    private static final UUID LEDGER = UUID.randomUUID();

    private final SpeakingSubmissionRepository submissions = mock(SpeakingSubmissionRepository.class);
    private final AccessClient points = mock(AccessClient.class);
    private final SpeakingPathUpdater pathUpdater = mock(SpeakingPathUpdater.class);
    private final LearnerLock lock = mock(LearnerLock.class);
    private SettleSpeakingSubmissionUseCase useCase;
    private SpeakingSubmission submission;

    @BeforeEach
    void setUp() {
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        useCase = new SettleSpeakingSubmissionUseCase(submissions, points, pathUpdater, lock,
                SpeakingSettingsFixture.enabled(), new ObjectMapper(), transactions);
        submission = mock(SpeakingSubmission.class);
        when(submission.id()).thenReturn(SUBMISSION);
        when(submission.userId()).thenReturn(USER);
        when(submission.requestId()).thenReturn(REQUEST);
        when(submission.pointCost()).thenReturn(5);
        when(submissions.findOwned(SUBMISSION, USER)).thenReturn(Optional.of(submission));
        when(submissions.findForUpdate(SUBMISSION)).thenReturn(Optional.of(submission));
    }

    @Test
    void debitsOnceWithTheLearnersKeyThenReleasesTheGrade() {
        when(submission.status()).thenReturn(SpeakingSubmissionStatus.PAYMENT_PENDING);
        when(points.debit(eq(USER), eq(5), eq("SPEAKING_AI"), eq(SUBMISSION), eq("speaking:" + USER + ":" + REQUEST), anyString()))
                .thenReturn(LEDGER);

        useCase.execute(USER, SUBMISSION);

        verify(submission).graded(eq(LEDGER), any());
        verify(submissions).save(submission);
    }

    @Test
    void notEnoughPointsLeavesTheGradeWaiting() {
        when(submission.status()).thenReturn(SpeakingSubmissionStatus.PAYMENT_PENDING);
        when(points.debit(any(), anyInt(), any(), any(), any(), any())).thenThrow(new InsufficientPointsException());

        assertThatThrownBy(() -> useCase.execute(USER, SUBMISSION)).isInstanceOf(InsufficientPointsException.class);
        verify(submission, never()).graded(any(), any());
    }

    @Test
    void anUnreachablePointsServiceChargesNothing() {
        when(submission.status()).thenReturn(SpeakingSubmissionStatus.PAYMENT_PENDING);
        when(points.debit(any(), anyInt(), any(), any(), any(), any())).thenThrow(new AccessUnavailableException(503));

        assertThatThrownBy(() -> useCase.execute(USER, SUBMISSION)).isInstanceOf(AccessUnavailableException.class);
        verify(submission, never()).graded(any(), any());
    }

    @Test
    void aSubmissionThatIsNotWaitingForPaymentIsReturnedUntouched() {
        when(submission.status()).thenReturn(SpeakingSubmissionStatus.GRADED);

        assertThat(useCase.execute(USER, SUBMISSION).status()).isEqualTo(SpeakingSubmissionStatus.GRADED);
        verify(points, never()).debit(any(), anyInt(), any(), any(), any(), any());
    }

    @Test
    void anotherLearnersSubmissionIsNotFound() {
        UUID stranger = UUID.randomUUID();
        when(submissions.findOwned(SUBMISSION, stranger)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(stranger, SUBMISSION))
                .isInstanceOf(LearningRequestException.class).hasMessageContaining("not found");
        verify(points, never()).debit(any(), anyInt(), any(), any(), any(), any());
    }
}
