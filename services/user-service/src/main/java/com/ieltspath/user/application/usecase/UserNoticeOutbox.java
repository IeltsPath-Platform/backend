package com.ieltspath.user.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.user.domain.aggregate.OutboxEvent;
import com.ieltspath.user.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Writes an account notice (password changed, Google linked) to the outbox. Call it inside the transaction that makes
 * the change, so a rolled-back change sends nothing.
 */
@Component
@RequiredArgsConstructor
public class UserNoticeOutbox {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void enqueue(UUID eventId, UUID userId, String eventType, Object event) {
        try {
            outboxEventRepository.save(OutboxEvent.create(eventId, userId, eventType, objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException serializationFailure) {
            throw new IllegalStateException("Could not serialize user notice event", serializationFailure);
        }
    }
}
