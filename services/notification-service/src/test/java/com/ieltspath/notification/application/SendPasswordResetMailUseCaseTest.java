package com.ieltspath.notification.application;

import com.ieltspath.events.user.PasswordResetRequestedV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SendPasswordResetMailUseCaseTest {
    private PasswordResetMailer mailer;
    private MailDeliveryRepository deliveries;
    private SendPasswordResetMailUseCase useCase;

    @BeforeEach
    void setUp() {
        mailer = mock(PasswordResetMailer.class);
        deliveries = mock(MailDeliveryRepository.class);
        useCase = new SendPasswordResetMailUseCase(mailer, deliveries);
        ReflectionTestUtils.setField(useCase, "frontendBaseUrl", "https://learn.example.test/");
    }

    @Test
    void sendsLinkAndRecordsDelivery() {
        UUID id = UUID.randomUUID();
        when(deliveries.exists(id)).thenReturn(false);
        useCase.execute(event(id, Instant.now().plusSeconds(600)));
        verify(mailer).send("learner@example.test", "Learner", "https://learn.example.test/reset-password?token=reset-token");
        verify(deliveries).recordSent(any(), any());
    }

    @Test
    void skipsExpiredAndAlreadyDeliveredEvents() {
        UUID expired = UUID.randomUUID();
        when(deliveries.exists(expired)).thenReturn(false);
        useCase.execute(event(expired, Instant.now().minusSeconds(1)));
        UUID duplicate = UUID.randomUUID();
        when(deliveries.exists(duplicate)).thenReturn(true);
        useCase.execute(event(duplicate, Instant.now().plusSeconds(600)));
        verify(mailer, never()).send(any(), any(), any());
        verify(deliveries, never()).recordSent(any(), any());
    }

    @Test
    void rejectsInvalidContract() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new PasswordResetRequestedV1(
                UUID.randomUUID(), "wrong.event.v1", Instant.now(), UUID.randomUUID(), "x", "", "t", Instant.now().plusSeconds(10))));
    }

    private PasswordResetRequestedV1 event(UUID id, Instant expiresAt) {
        return new PasswordResetRequestedV1(id, PasswordResetRequestedV1.EVENT_TYPE, Instant.now(), UUID.randomUUID(),
                "learner@example.test", "Learner", "reset-token", expiresAt);
    }
}
