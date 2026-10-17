package com.ieltspath.learning.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.service.RewardGrantService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PointsGrantSettledListenerTest {
    private final RewardGrantService grants = mock(RewardGrantService.class);
    private final PointsGrantSettledListener listener = new PointsGrantSettledListener(grants, new ObjectMapper());

    private static Message body(String json) {
        return MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8)).build();
    }

    @Test
    void aGrantedAnswerIsRecorded() {
        UUID id = UUID.randomUUID();

        listener.onSettled(body("{\"grantId\":\"" + id + "\",\"status\":\"GRANTED\",\"error\":null}"));

        verify(grants).settle(id, "GRANTED", null);
    }

    @Test
    void aFailedAnswerKeepsItsError() {
        UUID id = UUID.randomUUID();

        listener.onSettled(body("{\"grantId\":\"" + id + "\",\"status\":\"FAILED\",\"error\":\"INVALID_PAYLOAD\"}"));

        verify(grants).settle(id, "FAILED", "INVALID_PAYLOAD");
    }

    @Test
    void anUnreadableMessageIsDroppedNotRequeued() {
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.onSettled(body("not json")));
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> listener.onSettled(body("{\"grantId\":\"nope\",\"status\":\"GRANTED\"}")));
        verifyNoInteractions(grants);
    }
}
