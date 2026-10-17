package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.GateEssayJobStore;
import com.ieltspath.assessment.application.port.HumanGradingRequestStore;
import com.ieltspath.assessment.infrastructure.client.AccessHumanGradingCreditClient;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestHumanGradingUseCaseTest {
    private final HumanGradingRequestStore requests = mock(HumanGradingRequestStore.class);
    private final GateEssayJobStore jobs = mock(GateEssayJobStore.class);
    private final AccessHumanGradingCreditClient credits = mock(AccessHumanGradingCreditClient.class);
    private final RequestHumanGradingUseCase useCase = new RequestHumanGradingUseCase(requests, jobs, credits);
    private final UUID user = UUID.randomUUID();
    private final UUID attempt = UUID.randomUUID();
    private final UUID item = UUID.randomUUID();
    private final UUID submission = UUID.randomUUID();

    @Test
    void consumesCreditAndQueuesOneIdempotentHumanJob() {
        when(requests.submittedTopicEssay(user, attempt, item)).thenReturn(Optional.of(submission));

        var result = useCase.execute(user, attempt, item);

        assertEquals(new RequestHumanGradingUseCase.RequestResult(submission, "QUEUED"), result);
        verify(credits).consume(submission);
        verify(jobs).enqueueHuman(submission, user, "human-credit:" + submission, "HUMAN_CREDIT");
        verifyNoMoreInteractions(credits);
    }

    @Test
    void refundsCreditWhenHumanJobCannotBeQueued() {
        when(requests.submittedTopicEssay(user, attempt, item)).thenReturn(Optional.of(submission));
        doThrow(new IllegalStateException("database unavailable")).when(jobs)
                .enqueueHuman(submission, user, "human-credit:" + submission, "HUMAN_CREDIT");

        assertThrows(IllegalStateException.class, () -> useCase.execute(user, attempt, item));

        verify(credits).consume(submission);
        verify(credits).refund(submission);
    }
}
