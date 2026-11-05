package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.GateEssayJobStore;
import com.ieltspath.assessment.application.port.HumanGradingCreditPort;
import com.ieltspath.assessment.application.port.HumanGradingRequestStore;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class HumanGradingCreditFailureWindowTest {
    private final HumanGradingRequestStore requests = mock(HumanGradingRequestStore.class);
    private final GateEssayJobStore jobs = mock(GateEssayJobStore.class);
    private final HumanGradingCreditPort credits = mock(HumanGradingCreditPort.class);
    private final RequestHumanGradingUseCase useCase = new RequestHumanGradingUseCase(requests, jobs, credits,
            TransactionOperations.withoutTransaction());
    private final UUID user = UUID.randomUUID();
    private final UUID attempt = UUID.randomUUID();
    private final UUID item = UUID.randomUUID();
    private final UUID submission = UUID.randomUUID();
    private final String key = "human-credit:" + submission;

    private void essayExists() {
        when(requests.submittedTopicEssay(user, attempt, item)).thenReturn(Optional.of(submission));
    }

    @Test
    void jobCreationFailureRefundsOnce() {
        essayExists();
        when(credits.consume(submission)).thenReturn(true);
        doThrow(new IllegalStateException("database unavailable")).when(jobs)
                .enqueueHuman(submission, user, key, "HUMAN_CREDIT");

        assertThatThrownBy(() -> useCase.execute(user, attempt, item)).hasMessageContaining("database unavailable");

        verify(credits).consume(submission);
        verify(credits).refund(submission);
    }

    @Test
    void refundFailureKeepsCreditSpentAndRetryDoesNotChargeAgain() {
        essayExists();
        when(credits.consume(submission)).thenReturn(true, false);
        doThrow(new IllegalStateException("database unavailable")).doNothing().when(jobs)
                .enqueueHuman(submission, user, key, "HUMAN_CREDIT");
        doThrow(new IllegalStateException("access unavailable")).when(credits).refund(submission);

        assertThatThrownBy(() -> useCase.execute(user, attempt, item)).hasMessageContaining("database unavailable");
        assertThat(useCase.execute(user, attempt, item).status()).isEqualTo("QUEUED");

        verify(credits, times(2)).consume(submission);
        verify(credits).refund(submission);
    }

    @Test
    void uncertainJobKeepsCredit() {
        essayExists();
        when(credits.consume(submission)).thenReturn(true);
        doThrow(new IllegalStateException("connection lost after commit")).when(jobs)
                .enqueueHuman(submission, user, key, "HUMAN_CREDIT");
        when(jobs.jobExists(key)).thenReturn(false, true);

        assertThatThrownBy(() -> useCase.execute(user, attempt, item))
                .hasMessageContaining("connection lost after commit");
        assertThat(useCase.execute(user, attempt, item).status()).isEqualTo("QUEUED");

        verify(credits).consume(submission);
        verify(credits, never()).refund(submission);
    }
}
