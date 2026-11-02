package com.ieltspath.access.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ieltspath.access.application.command.GrantSubscriptionCommand;
import com.ieltspath.access.domain.aggregate.Plan;
import com.ieltspath.access.domain.entity.OutboxEvent;
import com.ieltspath.access.domain.repository.OutboxEventRepository;
import com.ieltspath.access.domain.repository.PlanRepository;
import com.ieltspath.access.domain.repository.SubscriptionRepository;
import com.ieltspath.events.access.SubscriptionGrantedV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrantSubscriptionUseCaseTest {
    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final PlanRepository plans = mock(PlanRepository.class);
    private final OutboxEventRepository outbox = mock(OutboxEventRepository.class);
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final GrantSubscriptionUseCase useCase = new GrantSubscriptionUseCase(subscriptions, plans, outbox, mapper);
    private final Plan plan = Plan.create("PREMIUM_MONTHLY", "Premium monthly");
    private final UUID learner = UUID.randomUUID();

    @BeforeEach
    void stubRepositories() {
        when(plans.findById(plan.getId())).thenReturn(Optional.of(plan));
        when(subscriptions.findActiveByUserId(learner)).thenReturn(Optional.empty());
        when(subscriptions.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void savesOnePendingSubscriptionGrantedEvent() {
        useCase.execute(new GrantSubscriptionCommand(learner, plan.getId(), 30, 0));

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outbox).save(saved.capture());
        assertEquals("SubscriptionGranted", saved.getValue().getEventType());
        assertEquals("PENDING", saved.getValue().getStatus());
    }

    @Test
    void payloadMatchesSubscriptionGrantedV1() throws Exception {
        useCase.execute(new GrantSubscriptionCommand(learner, plan.getId(), 30, 0));

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outbox).save(saved.capture());
        OutboxEvent event = saved.getValue();
        SubscriptionGrantedV1 payload = mapper.readValue(event.getPayload(), SubscriptionGrantedV1.class);

        assertEquals(event.getId(), payload.eventId(), "the payload carries the outbox row id as the event id");
        assertEquals(SubscriptionGrantedV1.EVENT_TYPE, payload.eventType());
        assertEquals(learner, payload.userId());
        assertEquals("PREMIUM_MONTHLY", payload.planCode());
        assertTrue(payload.endsAt().isAfter(payload.occurredAt()));
        assertEquals(event.getAggregateId(), payload.subscriptionId());
    }
}
