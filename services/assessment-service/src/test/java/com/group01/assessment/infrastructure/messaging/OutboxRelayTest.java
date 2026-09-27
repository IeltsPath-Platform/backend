package com.group01.assessment.infrastructure.messaging;

import com.group01.assessment.domain.entity.OutboxEvent;
import com.group01.assessment.domain.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {
    @Mock OutboxEventRepository outbox;
    @Mock OutboxEventPublisher publisher;

    @Test
    void confirmedEventIsMarkedPublished() {
        OutboxEvent event = event();
        when(outbox.claimUnpublished(50, 20)).thenReturn(List.of(event));

        int published = new OutboxRelay(outbox, publisher, 50, 20).publishPendingBatch();

        assertEquals(1, published);
        verify(publisher).publish(event);
        verify(outbox).markPublished(eq(event.id()), any(Instant.class));
        verify(outbox, never()).recordFailure(any(), any());
    }

    @Test
    void unconfirmedEventStaysPendingWithItsFailureRecordedAndTheBatchContinues() {
        OutboxEvent failing = event();
        OutboxEvent next = event();
        when(outbox.claimUnpublished(50, 20)).thenReturn(List.of(failing, next));
        doThrow(new OutboxEventPublisher.PublishFailedException("Broker did not confirm the event"))
                .when(publisher).publish(failing);

        int published = new OutboxRelay(outbox, publisher, 50, 20).publishPendingBatch();

        assertEquals(1, published);
        verify(outbox).recordFailure(failing.id(), "Broker did not confirm the event");
        verify(outbox, never()).markPublished(eq(failing.id()), any());
        verify(outbox).markPublished(eq(next.id()), any(Instant.class));
    }

    private static OutboxEvent event() {
        return OutboxEvent.pending(UUID.randomUUID(), "AssessmentResult", UUID.randomUUID().toString(),
                "AssessmentCompleted.v2", "{}", Instant.now());
    }
}
