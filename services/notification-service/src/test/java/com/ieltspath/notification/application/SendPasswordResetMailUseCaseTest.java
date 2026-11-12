package com.ieltspath.notification.application;

import com.ieltspath.events.user.PasswordResetRequestedV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SendPasswordResetMailUseCaseTest {
    private static final RenderedMail RENDERED = new RenderedMail("subject", "<p>html</p>", "text");

    private MailTemplateRenderer renderer;
    private MailSender sender;
    private MailDeliveryRepository deliveries;
    private SendPasswordResetMailUseCase useCase;

    @BeforeEach
    void setUp() {
        renderer = mock(MailTemplateRenderer.class);
        sender = mock(MailSender.class);
        deliveries = mock(MailDeliveryRepository.class);
        when(renderer.render(any(), any())).thenReturn(RENDERED);
        useCase = new SendPasswordResetMailUseCase(renderer, sender, deliveries);
        ReflectionTestUtils.setField(useCase, "frontendBaseUrl", "https://learn.example.test/");
        ReflectionTestUtils.setField(useCase, "enabled", true);
    }

    @Test
    void rendersTemplateWithLinkAndRecordsDeliveryPerTemplate() {
        UUID id = UUID.randomUUID();
        when(deliveries.exists(id, "password-reset")).thenReturn(false);

        useCase.execute(event(id, Instant.now().plusSeconds(600)));

        verify(renderer).render(eq(MailTemplate.PASSWORD_RESET), argThatModelContains("https://learn.example.test/reset-password?token=reset-token"));
        verify(sender).send("learner@example.test", RENDERED);
        verify(deliveries).recordSent(eq(id), eq("password-reset"), any());
    }

    @Test
    void skipsExpiredAndAlreadyDeliveredEvents() {
        UUID expired = UUID.randomUUID();
        useCase.execute(event(expired, Instant.now().minusSeconds(1)));

        UUID duplicate = UUID.randomUUID();
        when(deliveries.exists(duplicate, "password-reset")).thenReturn(true);
        useCase.execute(event(duplicate, Instant.now().plusSeconds(600)));

        verify(sender, never()).send(any(), any());
        verify(deliveries, never()).recordSent(any(), any(), any());
    }

    @Test
    void skipsWhenTemplateIsDisabled() {
        ReflectionTestUtils.setField(useCase, "enabled", false);
        useCase.execute(event(UUID.randomUUID(), Instant.now().plusSeconds(600)));
        verify(sender, never()).send(any(), any());
    }

    @Test
    void rejectsInvalidContract() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new PasswordResetRequestedV1(
                UUID.randomUUID(), "wrong.event.v1", Instant.now(), UUID.randomUUID(), "x", "", "t", Instant.now().plusSeconds(10))));
    }

    private static Map<String, Object> argThatModelContains(String link) {
        return org.mockito.ArgumentMatchers.argThat(model -> link.equals(model.get("resetLink")));
    }

    private PasswordResetRequestedV1 event(UUID id, Instant expiresAt) {
        return new PasswordResetRequestedV1(id, PasswordResetRequestedV1.EVENT_TYPE, Instant.now(), UUID.randomUUID(),
                "learner@example.test", "Learner", "reset-token", expiresAt);
    }
}
