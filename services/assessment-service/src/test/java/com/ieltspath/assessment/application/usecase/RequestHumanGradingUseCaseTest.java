package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.GateEssayJobStore;
import com.ieltspath.assessment.application.port.HumanGradingCreditPort;
import com.ieltspath.assessment.application.port.HumanGradingRequestStore;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestHumanGradingUseCaseTest {
    private final HumanGradingRequestStore requests = mock(HumanGradingRequestStore.class);
    private final GateEssayJobStore jobs = mock(GateEssayJobStore.class);
    private final HumanGradingCreditPort credits = mock(HumanGradingCreditPort.class);
    private final RequestHumanGradingUseCase useCase =
            new RequestHumanGradingUseCase(requests, jobs, credits, TransactionOperations.withoutTransaction());
    private final UUID user = UUID.randomUUID();
    private final UUID attempt = UUID.randomUUID();
    private final UUID item = UUID.randomUUID();
    private final UUID submission = UUID.randomUUID();
    private final String key = "human-credit:" + submission;

    private void essayExists() {
        when(requests.submittedTopicEssay(user, attempt, item)).thenReturn(Optional.of(submission));
    }

    @Test
    void consumesCreditAndQueuesOneIdempotentHumanJob() {
        essayExists();
        when(credits.consume(submission)).thenReturn(true);

        var result = useCase.execute(user, attempt, item);

        assertEquals(new RequestHumanGradingUseCase.RequestResult(submission, "QUEUED"), result);
        verify(credits).consume(submission);
        verify(jobs).enqueueHuman(submission, user, key, "HUMAN_CREDIT");
        verify(credits, never()).refund(submission);
    }

    @Test
    void refundsTheCreditThisCallSpentWhenTheJobCannotBeQueued() {
        essayExists();
        when(credits.consume(submission)).thenReturn(true);
        doThrow(new IllegalStateException("database unavailable")).when(jobs)
                .enqueueHuman(submission, user, key, "HUMAN_CREDIT");

        assertThrows(IllegalStateException.class, () -> useCase.execute(user, attempt, item));

        verify(credits).refund(submission);
    }

    @Test
    void anAskForAJobThatAlreadyExistsNeitherSpendsNorRefunds() {
        essayExists();
        when(jobs.jobExists(key)).thenReturn(true);

        var result = useCase.execute(user, attempt, item);

        assertEquals("QUEUED", result.status());
        verify(credits, never()).consume(submission);
        verify(credits, never()).refund(submission);
        verify(jobs, never()).enqueueHuman(submission, user, key, "HUMAN_CREDIT");
    }

    @Test
    void aTemporaryFailureOnTheRepeatDoesNotGiveBackTheCreditOfTheEarlierRequest() {
        essayExists();
        when(credits.consume(submission)).thenReturn(false);
        doThrow(new IllegalStateException("database unavailable")).when(jobs)
                .enqueueHuman(submission, user, key, "HUMAN_CREDIT");

        assertThrows(IllegalStateException.class, () -> useCase.execute(user, attempt, item));

        verify(credits, never()).refund(submission);
    }

    @Test
    void aJobThatWasWrittenBeforeTheFailureKeepsItsCredit() {
        essayExists();
        when(credits.consume(submission)).thenReturn(true);
        doThrow(new IllegalStateException("connection lost after commit")).when(jobs)
                .enqueueHuman(submission, user, key, "HUMAN_CREDIT");
        when(jobs.jobExists(key)).thenReturn(false, true);

        assertThrows(IllegalStateException.class, () -> useCase.execute(user, attempt, item));

        verify(credits, never()).refund(submission);
    }

    @Test
    void aRefundThatFailsDoesNotHideTheOriginalFailureAndAskingAgainReusesTheCredit() {
        essayExists();
        when(credits.consume(submission)).thenReturn(true, false);
        doThrow(new IllegalStateException("database unavailable")).doNothing().when(jobs)
                .enqueueHuman(submission, user, key, "HUMAN_CREDIT");
        doThrow(new IllegalStateException("access down")).when(credits).refund(submission);

        assertThrows(IllegalStateException.class, () -> useCase.execute(user, attempt, item));
        var second = useCase.execute(user, attempt, item);

        assertEquals("QUEUED", second.status());
        verify(credits, org.mockito.Mockito.times(2)).consume(submission);
    }
}
