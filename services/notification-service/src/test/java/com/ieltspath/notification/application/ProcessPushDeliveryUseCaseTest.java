package com.ieltspath.notification.application;

import com.ieltspath.commonresilience.ResilientCall;
import com.ieltspath.notification.application.port.PushSendException;
import com.ieltspath.notification.application.port.PushSender;
import com.ieltspath.notification.application.port.PushTransientException;
import com.ieltspath.notification.domain.repository.PushDeliveryRepository;
import com.ieltspath.notification.domain.vo.Notification;
import com.ieltspath.notification.domain.vo.PushDeliveryJob;
import com.ieltspath.notification.domain.vo.PushSendSnapshot;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessPushDeliveryUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");

    @Test
    void acceptedPushMarksOnlyTheCurrentClaimSent() {
        PushDeliveryRepository repository = mock(PushDeliveryRepository.class);
        PushDeliveryJob job = job(1);
        when(repository.claimDue(eq(NOW), eq(2), eq(NOW.plusSeconds(120)), eq(8))).thenReturn(List.of(job));
        when(repository.loadCurrentBinding(job)).thenReturn(snapshot(job));
        PushSender sender = sender(() -> "fcm-message-id");
        var useCase = useCase(repository, sender);

        useCase.processBatch();

        verify(repository).markSent(job, NOW, "fcm-message-id");
        verify(repository, never()).scheduleRetry(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unregisteredTokenIsDisabledAndTheDeliveryIsCancelled() {
        PushDeliveryRepository repository = mock(PushDeliveryRepository.class);
        PushDeliveryJob job = job(1);
        PushSendSnapshot snapshot = snapshot(job);
        when(repository.claimDue(eq(NOW), eq(2), eq(NOW.plusSeconds(120)), eq(8))).thenReturn(List.of(job));
        when(repository.loadCurrentBinding(job)).thenReturn(snapshot);
        PushSender sender = sender(() -> { throw new PushSendException(PushSendException.Category.UNREGISTERED, "UNREGISTERED"); });

        useCase(repository, sender).processBatch();

        verify(repository).disableInvalidBindingAndCancel(job, snapshot.tokenHash(), NOW);
        verify(repository, never()).scheduleRetry(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void quotaRetryHonorsProviderDelayWithoutSleepingInTheWorker() {
        PushDeliveryRepository repository = mock(PushDeliveryRepository.class);
        PushDeliveryJob job = job(1);
        when(repository.claimDue(eq(NOW), eq(2), eq(NOW.plusSeconds(120)), eq(8))).thenReturn(List.of(job));
        when(repository.loadCurrentBinding(job)).thenReturn(snapshot(job));
        PushSender sender = sender(() -> { throw new PushTransientException("QUOTA_EXCEEDED", 60); });

        useCase(repository, sender).processBatch();

        verify(repository).scheduleRetry(job, NOW, NOW.plusSeconds(60), "QUOTA_EXCEEDED");
    }

    @Test
    void unavailableBindingCancelsWithoutCallingFirebase() {
        PushDeliveryRepository repository = mock(PushDeliveryRepository.class);
        PushDeliveryJob job = job(1);
        when(repository.claimDue(eq(NOW), eq(2), eq(NOW.plusSeconds(120)), eq(8))).thenReturn(List.of(job));
        when(repository.loadCurrentBinding(job)).thenReturn(null);
        PushSender sender = org.mockito.Mockito.mock(PushSender.class);
        when(sender.isReady()).thenReturn(true);

        useCase(repository, sender).processBatch();

        verify(sender, never()).send(org.mockito.ArgumentMatchers.any());
        verify(repository).cancel(job, NOW, "STALE_BINDING");
    }

    private ProcessPushDeliveryUseCase useCase(PushDeliveryRepository repository, PushSender sender) {
        return new ProcessPushDeliveryUseCase(repository, sender, ResilientCall.isolatedDefault(),
                new SimpleMeterRegistry(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private PushSender sender(java.util.function.Supplier<String> action) {
        return new PushSender() {
            @Override public boolean isReady() { return true; }
            @Override public String send(PushSendSnapshot ignored) { return action.get(); }
        };
    }

    private PushDeliveryJob job(int attempt) {
        return new PushDeliveryJob(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                3, attempt, UUID.randomUUID(), NOW.plusSeconds(24 * 60 * 60));
    }

    private PushSendSnapshot snapshot(PushDeliveryJob job) {
        Notification notification = new Notification(job.notificationId(), job.recipientId(), job.recipientId(),
                "community.user-followed.v1", null, null, null, NOW, null);
        return new PushSendSnapshot(job, notification, "sensitive-token", "sha256-token-hash");
    }
}
