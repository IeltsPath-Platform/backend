package com.ieltspath.notification.application;

import com.ieltspath.events.user.GoogleAccountLinkedV1;
import com.ieltspath.events.user.GoogleAccountUnlinkedV1;
import com.ieltspath.events.user.PasswordResetGoogleOnlyV1;
import com.ieltspath.events.user.PasswordChangedV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SendAccountNoticeMailUseCaseTest {
    private static final RenderedMail RENDERED = new RenderedMail("subject", "<p>html</p>", "text");

    private MailTemplateRenderer renderer;
    private MailSender sender;
    private MailDeliveryRepository deliveries;
    private SendAccountNoticeMailUseCase useCase;

    @BeforeEach
    void setUp() {
        renderer = mock(MailTemplateRenderer.class);
        sender = mock(MailSender.class);
        deliveries = mock(MailDeliveryRepository.class);
        when(renderer.render(any(), anyMap())).thenReturn(RENDERED);
        useCase = new SendAccountNoticeMailUseCase(renderer, sender, deliveries);
        ReflectionTestUtils.setField(useCase, "passwordChangedEnabled", true);
        ReflectionTestUtils.setField(useCase, "googleAccountLinkedEnabled", true);
        ReflectionTestUtils.setField(useCase, "googleAccountUnlinkedEnabled", true);
        ReflectionTestUtils.setField(useCase, "passwordResetGoogleOnlyEnabled", true);
    }

    @Test
    void passwordChangedSendsToEventAddressAndRecordsDelivery() {
        UUID eventId = UUID.randomUUID();
        when(deliveries.exists(eventId, "password-changed")).thenReturn(false);

        useCase.passwordChanged(passwordChanged(eventId, "Nguyễn An"));

        verify(renderer).render(eq(MailTemplate.PASSWORD_CHANGED), anyMap());
        verify(sender).send("learner@example.test", RENDERED);
        verify(deliveries).recordSent(eq(eventId), eq("password-changed"), any());
    }

    @Test
    void blankNameFallsBackToGenericGreeting() {
        UUID eventId = UUID.randomUUID();
        when(deliveries.exists(eventId, "password-changed")).thenReturn(false);

        useCase.passwordChanged(passwordChanged(eventId, " "));

        verify(renderer).render(eq(MailTemplate.PASSWORD_CHANGED),
                org.mockito.ArgumentMatchers.argThat(model -> "bạn".equals(model.get("displayName"))));
    }

    @Test
    void googleAccountLinkedSendsOnceAndSkipsDuplicate() {
        UUID eventId = UUID.randomUUID();
        when(deliveries.exists(eventId, "google-account-linked")).thenReturn(false);
        useCase.googleAccountLinked(googleLinked(eventId));
        verify(sender).send("learner@example.test", RENDERED);

        when(deliveries.exists(eventId, "google-account-linked")).thenReturn(true);
        useCase.googleAccountLinked(googleLinked(eventId));
        verify(sender, org.mockito.Mockito.times(1)).send(any(), any());
    }

    @Test
    void googleAccountUnlinkedSendsOnceAndSkipsDuplicate() {
        UUID eventId = UUID.randomUUID();
        when(deliveries.exists(eventId, "google-account-unlinked")).thenReturn(false);
        useCase.googleAccountUnlinked(googleUnlinked(eventId));
        verify(renderer).render(eq(MailTemplate.GOOGLE_ACCOUNT_UNLINKED), anyMap());
        verify(sender).send("learner@example.test", RENDERED);

        when(deliveries.exists(eventId, "google-account-unlinked")).thenReturn(true);
        useCase.googleAccountUnlinked(googleUnlinked(eventId));
        verify(sender, org.mockito.Mockito.times(1)).send(any(), any());
    }


    @Test
    void passwordResetGoogleOnlySendsTheGoogleNoticeOnceAndSkipsDuplicate() {
        UUID eventId = UUID.randomUUID();
        var event = new PasswordResetGoogleOnlyV1(eventId, PasswordResetGoogleOnlyV1.EVENT_TYPE, Instant.now(),
                UUID.randomUUID(), "learner@example.test", "An");
        when(deliveries.exists(eventId, "password-reset-google-only")).thenReturn(false);
        useCase.passwordResetGoogleOnly(event);
        verify(renderer).render(eq(MailTemplate.PASSWORD_RESET_GOOGLE_ONLY), anyMap());
        verify(sender).send("learner@example.test", RENDERED);

        when(deliveries.exists(eventId, "password-reset-google-only")).thenReturn(true);
        useCase.passwordResetGoogleOnly(event);
        verify(sender, org.mockito.Mockito.times(1)).send(any(), any());
    }

    @Test
    void passwordResetGoogleOnlyRejectsAnEventWithoutAddress() {
        var noAddress = new PasswordResetGoogleOnlyV1(UUID.randomUUID(), PasswordResetGoogleOnlyV1.EVENT_TYPE,
                Instant.now(), UUID.randomUUID(), " ", "An");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> useCase.passwordResetGoogleOnly(noAddress));
        verify(sender, never()).send(any(), any());
    }

    @Test
    void disabledTemplateSendsNothing() {
        ReflectionTestUtils.setField(useCase, "passwordChangedEnabled", false);
        useCase.passwordChanged(passwordChanged(UUID.randomUUID(), "An"));
        verify(sender, never()).send(any(), any());
    }

    @Test
    void rejectsEventWithoutAddress() {
        PasswordChangedV1 noAddress = new PasswordChangedV1(UUID.randomUUID(), PasswordChangedV1.EVENT_TYPE,
                Instant.now(), UUID.randomUUID(), " ", "An");
        assertThrows(IllegalArgumentException.class, () -> useCase.passwordChanged(noAddress));
    }

    @Test
    void emailVerificationLinkIsBuiltFromTheFrontendUrlAndEncodesTheToken() {
        ReflectionTestUtils.setField(useCase, "frontendBaseUrl", "https://learn.example.test/");
        ReflectionTestUtils.setField(useCase, "emailVerificationEnabled", true);
        UUID eventId = UUID.randomUUID();
        when(deliveries.exists(eventId, "email-verification")).thenReturn(false);

        useCase.emailVerificationRequested(new com.ieltspath.events.user.EmailVerificationRequestedV1(eventId,
                com.ieltspath.events.user.EmailVerificationRequestedV1.EVENT_TYPE, Instant.now(), UUID.randomUUID(),
                "learner@example.test", "An", "tok en", Instant.now().plusSeconds(3600)));

        verify(renderer).render(eq(MailTemplate.EMAIL_VERIFICATION),
                org.mockito.ArgumentMatchers.argThat(model ->
                        "https://learn.example.test/verify-email?token=tok+en".equals(model.get("verifyLink"))));
        verify(sender).send(eq("learner@example.test"), any());
    }

    @Test
    void subscriptionExpiryUsesTheExpiringTemplateBeforeEndAndTheExpiredTemplateAfter() {
        ReflectionTestUtils.setField(useCase, "subscriptionExpiringEnabled", true);
        ReflectionTestUtils.setField(useCase, "subscriptionExpiredEnabled", true);
        UUID soon = UUID.randomUUID();
        UUID ended = UUID.randomUUID();
        when(deliveries.exists(soon, "subscription-expiring")).thenReturn(false);
        when(deliveries.exists(ended, "subscription-expired")).thenReturn(false);

        useCase.subscriptionExpiry(new com.ieltspath.events.access.SubscriptionExpiryNoticeV1(soon,
                com.ieltspath.events.access.SubscriptionExpiryNoticeV1.EVENT_TYPE, Instant.now(), UUID.randomUUID(),
                UUID.randomUUID(), "ONE_DAY", Instant.now().plusSeconds(3600), "learner@example.test", "An"));
        useCase.subscriptionExpiry(new com.ieltspath.events.access.SubscriptionExpiryNoticeV1(ended,
                com.ieltspath.events.access.SubscriptionExpiryNoticeV1.EVENT_TYPE, Instant.now(), UUID.randomUUID(),
                UUID.randomUUID(), "EXPIRED", Instant.now().minusSeconds(60), "learner@example.test", "An"));

        verify(renderer).render(eq(MailTemplate.SUBSCRIPTION_EXPIRING), any());
        verify(renderer).render(eq(MailTemplate.SUBSCRIPTION_EXPIRED), any());
    }

    private PasswordChangedV1 passwordChanged(UUID eventId, String displayName) {
        return new PasswordChangedV1(eventId, PasswordChangedV1.EVENT_TYPE, Instant.now(), UUID.randomUUID(),
                "learner@example.test", displayName);
    }

    private GoogleAccountLinkedV1 googleLinked(UUID eventId) {
        return new GoogleAccountLinkedV1(eventId, GoogleAccountLinkedV1.EVENT_TYPE, Instant.now(), UUID.randomUUID(),
                "learner@example.test", "An");
    }

    private GoogleAccountUnlinkedV1 googleUnlinked(UUID eventId) {
        return new GoogleAccountUnlinkedV1(eventId, GoogleAccountUnlinkedV1.EVENT_TYPE, Instant.now(), UUID.randomUUID(),
                "learner@example.test", "An");
    }
}
