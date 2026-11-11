package com.ieltspath.access.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.access.domain.entity.OutboxEvent;
import com.ieltspath.access.domain.repository.OutboxEventRepository;
import com.ieltspath.access.domain.repository.SubscriptionExpiryNoticeRepository;
import com.ieltspath.access.domain.repository.SubscriptionExpiryNoticeRepository.DueSubscription;
import com.ieltspath.access.domain.repository.UserEmailVerificationRepository;
import com.ieltspath.access.domain.repository.UserEmailVerificationRepository.Contact;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionExpiryNoticeUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-11-10T00:00:00Z");

    private final SubscriptionExpiryNoticeRepository notices = mock(SubscriptionExpiryNoticeRepository.class);
    private final UserEmailVerificationRepository contacts = mock(UserEmailVerificationRepository.class);
    private final OutboxEventRepository outbox = mock(OutboxEventRepository.class);
    private final SubscriptionExpiryNoticeUseCase useCase = new SubscriptionExpiryNoticeUseCase(
            notices, contacts, outbox, new ObjectMapper().findAndRegisterModules());

    @Test
    void dueStagesAreListedMostUrgentFirst() {
        assertEquals(List.of("EXPIRED", "ONE_DAY", "SEVEN_DAYS"),
                SubscriptionExpiryNoticeUseCase.dueStages(NOW.minusSeconds(1), NOW));
        assertEquals(List.of("ONE_DAY", "SEVEN_DAYS"),
                SubscriptionExpiryNoticeUseCase.dueStages(NOW.plusSeconds(3600), NOW));
        assertEquals(List.of("SEVEN_DAYS"), SubscriptionExpiryNoticeUseCase.dueStages(NOW.plus(Duration.ofDays(3)), NOW));
        assertEquals(List.of(), SubscriptionExpiryNoticeUseCase.dueStages(NOW.plus(Duration.ofDays(10)), NOW));
    }

    @Test
    void queuesOnlyTheMostUrgentStageAndMarksTheEarlierOnesAsClaimed() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(notices.findActiveEndingBefore(any())).thenReturn(List.of(
                new DueSubscription(subscriptionId, userId, NOW.plusSeconds(3600))));
        when(contacts.findContact(userId)).thenReturn(Optional.of(new Contact("learner@example.test", "An")));
        when(notices.claim(eq(subscriptionId), anyString(), eq(NOW))).thenReturn(true);

        assertEquals(1, useCase.scan(NOW));

        verify(notices).claim(subscriptionId, "ONE_DAY", NOW);
        verify(notices).claim(subscriptionId, "SEVEN_DAYS", NOW);
        ArgumentCaptor<OutboxEvent> queued = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outbox).save(queued.capture());
        assertEquals("SubscriptionExpiryNotice", queued.getValue().getEventType());
        assertTrue(queued.getValue().getPayload().contains("\"stage\":\"ONE_DAY\""));
        assertTrue(queued.getValue().getPayload().contains("learner@example.test"));
    }

    @Test
    void aStageClaimedBeforeIsNotQueuedAgain() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(notices.findActiveEndingBefore(any())).thenReturn(List.of(
                new DueSubscription(subscriptionId, userId, NOW.minusSeconds(60))));
        when(contacts.findContact(userId)).thenReturn(Optional.of(new Contact("learner@example.test", "An")));
        when(notices.claim(any(), anyString(), any())).thenReturn(false);

        assertEquals(0, useCase.scan(NOW));
        verify(outbox, never()).save(any());
    }

    @Test
    void aSubscriptionWithoutAKnownAddressIsSkippedAndNotClaimed() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(notices.findActiveEndingBefore(any())).thenReturn(List.of(
                new DueSubscription(subscriptionId, userId, NOW.minusSeconds(60))));
        when(contacts.findContact(userId)).thenReturn(Optional.empty());

        assertEquals(0, useCase.scan(NOW));
        verify(notices, never()).claim(any(), anyString(), any());
        verify(outbox, never()).save(any());
    }
}
