package com.group01.learning.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.learning.application.usecase.ApplyAssessmentResultUseCase;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssessmentCompletedListenerTest {
    private final ApplyAssessmentResultUseCase apply = mock(ApplyAssessmentResultUseCase.class);
    private final Channel channel = mock(Channel.class);
    private final AssessmentCompletedListener listener =
            new AssessmentCompletedListener(new AssessmentCompletedParser(new ObjectMapper()), apply, 5);

    private static Message message(String body, long previousRejections) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(7L);
        properties.setMessageId("event-1");
        if (previousRejections > 0) {
            properties.setHeader("x-death", List.of(Map.of("queue", AssessmentMessagingConfig.QUEUE,
                    "reason", "rejected", "count", previousRejections)));
        }
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }

    @Test
    void appliedEventIsAcknowledged() throws Exception {
        listener.onMessage(message(AssessmentCompletedParserTest.event(""), 0), channel);
        verify(apply).execute(any());
        verify(channel).basicAck(7L, false);
    }

    @Test
    void contractViolationIsParkedWithAReasonAndAcknowledged() throws Exception {
        listener.onMessage(message("{\"event_type\":\"Other\"}", 0), channel);

        ArgumentCaptor<AMQP.BasicProperties> properties = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        verify(channel).basicPublish(eq(AssessmentMessagingConfig.DEAD_LETTER_EXCHANGE),
                eq(AssessmentMessagingConfig.DEAD_LETTER_QUEUE), eq(true), properties.capture(), any());
        assertTrue(properties.getValue().getHeaders().get(AssessmentCompletedListener.FAILURE_HEADER).toString()
                .startsWith("contract"));
        assertEquals("event-1", properties.getValue().getMessageId());
        verify(channel).basicAck(7L, false);
        verifyNoInteractions(apply);
    }

    @Test
    void transientFailureIsRetriedUntilTheLastAttemptIsParked() throws Exception {
        doThrow(new IllegalStateException("database down")).when(apply).execute(any());

        listener.onMessage(message(AssessmentCompletedParserTest.event(""), 3), channel);
        verify(channel).basicNack(7L, false, false);
        verify(channel, never()).basicPublish(anyString(), anyString(), anyBoolean(), any(), any());

        listener.onMessage(message(AssessmentCompletedParserTest.event(""), 4), channel);
        verify(channel).basicPublish(eq(AssessmentMessagingConfig.DEAD_LETTER_EXCHANGE),
                eq(AssessmentMessagingConfig.DEAD_LETTER_QUEUE), eq(true), any(), any());
        verify(channel).basicAck(7L, false);
    }

    @Test
    void failedParkingRetriesTheOriginalInsteadOfDroppingIt() throws Exception {
        doThrow(new java.io.IOException("broker gone")).when(channel)
                .basicPublish(anyString(), anyString(), anyBoolean(), any(), any());
        listener.onMessage(message("not json", 0), channel);
        verify(channel).basicNack(7L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
