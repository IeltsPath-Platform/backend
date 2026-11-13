package com.ieltspath.notification.application;

import com.ieltspath.notification.application.PushSender.Failure;
import com.ieltspath.notification.application.PushSender.PushSendException;
import com.ieltspath.notification.domain.repository.PushDeliveryRepository;
import com.ieltspath.notification.domain.repository.PushDeviceRepository;
import com.ieltspath.notification.domain.vo.PushDelivery;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DispatchPushDeliveriesUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-11-13T03:00:00Z");
    private final PushDeliveryRepository deliveries = mock(PushDeliveryRepository.class);
    private final PushDeviceRepository devices = mock(PushDeviceRepository.class);
    private final PushSender sender = mock(PushSender.class);
    private final DispatchPushDeliveriesUseCase useCase = new DispatchPushDeliveriesUseCase(deliveries, devices, sender,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private PushDelivery due(String token, int attempts) {
        return new PushDelivery(UUID.randomUUID(), attempts, token, UUID.randomUUID(), "assessment.grading-completed.v1",
                null, null, UUID.randomUUID());
    }

    @Test
    void sentPushCarriesVietnameseTextAndInboxIds() {
        var d = due("tok", 1);
        when(deliveries.claimDue(anyInt(), any())).thenReturn(List.of(d));
        when(sender.send(anyString(), anyString(), anyString(), any())).thenReturn("msg-1");

        useCase.dispatchDue();

        verify(sender).send("tok", "Bài viết đã được chấm", "Giám khảo đã chấm xong bài viết của bạn.", Map.of(
                "notificationId", d.notificationId().toString(), "eventType", d.eventType(),
                "gradingJobId", d.gradingJobId().toString()));
        verify(deliveries).markSent(d.id(), "msg-1");
    }

    @Test
    void revokedOrReassignedDeviceIsNeverSent() {
        var d = due(null, 1);
        when(deliveries.claimDue(anyInt(), any())).thenReturn(List.of(d));

        useCase.dispatchDue();

        verifyNoInteractions(sender);
        verify(deliveries).markFailed(d.id(), "DEVICE_GONE");
    }

    @Test
    void unregisteredTokenDropsTheDeviceWithoutRetry() {
        var d = due("dead", 1);
        when(deliveries.claimDue(anyInt(), any())).thenReturn(List.of(d));
        when(sender.send(anyString(), anyString(), anyString(), any()))
                .thenThrow(new PushSendException(Failure.TOKEN_INVALID, "UNREGISTERED"));

        useCase.dispatchDue();

        verify(devices).deleteByToken("dead");
        verify(deliveries).markFailed(d.id(), "UNREGISTERED");
        verify(deliveries, never()).retryAt(any(), any(), anyString());
    }

    @Test
    void transientFailureBacksOffExponentiallyThenGivesUp() {
        var third = due("tok", 3);
        var last = due("tok", DispatchPushDeliveriesUseCase.MAX_ATTEMPTS);
        when(deliveries.claimDue(anyInt(), any())).thenReturn(List.of(third, last));
        when(sender.send(anyString(), anyString(), anyString(), any()))
                .thenThrow(new PushSendException(Failure.TRANSIENT, "UNAVAILABLE"));

        useCase.dispatchDue();

        verify(deliveries).retryAt(third.id(), NOW.plusSeconds(120), "UNAVAILABLE");
        verify(deliveries).markFailed(last.id(), "UNAVAILABLE");
        verify(devices, never()).deleteByToken(anyString());
    }
}
