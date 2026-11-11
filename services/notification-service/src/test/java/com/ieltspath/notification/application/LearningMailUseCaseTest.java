package com.ieltspath.notification.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LearningMailUseCaseTest {
    private static final RenderedMail RENDERED = new RenderedMail("subject", "<p>html</p>", "text");
    private static final String SECRET = "test-secret-with-enough-length";

    private MailTemplateRenderer renderer;
    private MailSender sender;
    private MailDeliveryRepository deliveries;
    private LearningMailRepository repository;
    private LearningMailUseCase useCase;

    @BeforeEach
    void setUp() {
        renderer = mock(MailTemplateRenderer.class);
        sender = mock(MailSender.class);
        deliveries = mock(MailDeliveryRepository.class);
        repository = mock(LearningMailRepository.class);
        when(renderer.render(any(), anyMap())).thenReturn(RENDERED);
        when(repository.learningEnabled(any())).thenReturn(true);
        when(deliveries.reserveDailyNonSecuritySlot(any(), any(), any(), any(), any())).thenReturn(true);
        when(deliveries.reserveDailyNonSecuritySlot(any(), any(), any(), any(), any())).thenReturn(true);
        useCase = new LearningMailUseCase(renderer, sender, deliveries, repository, new MailUnsubscribeTokens(SECRET));
        ReflectionTestUtils.setField(useCase, "frontendBaseUrl", "https://learn.example.test/");
        ReflectionTestUtils.setField(useCase, "apiBaseUrl", "https://api.example.test");
    }

    @Test
    void examinerDigestSendsOneMailPerExaminerAndClaimsTheJobs() {
        UUID examiner = UUID.randomUUID();
        List<UUID> jobs = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(repository.examinersWithPendingJobs()).thenReturn(List.of(examiner));
        when(repository.claimPendingExaminerJobs(examiner)).thenReturn(jobs);
        when(repository.findContact(examiner)).thenReturn(Optional.of(new LearningMailRepository.Contact("ex@example.test", "Cô Lan")));

        assertEquals(1, useCase.flushExaminerDigests(Instant.now()));

        verify(sender).send(eq("ex@example.test"), eq(RENDERED));
        verify(repository, never()).addPendingExaminerJob(any(), any(), any());
    }

    @Test
    void examinerDigestKeepsTheJobsWhenTheDailyCapHoldsTheMail() {
        UUID examiner = UUID.randomUUID();
        List<UUID> jobs = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(repository.examinersWithPendingJobs()).thenReturn(List.of(examiner));
        when(repository.claimPendingExaminerJobs(examiner)).thenReturn(jobs);
        when(repository.findContact(examiner)).thenReturn(Optional.of(new LearningMailRepository.Contact("ex@example.test", "Cô Lan")));
        when(deliveries.reserveDailyNonSecuritySlot(any(), any(), any(), any(), any())).thenReturn(false);

        assertEquals(0, useCase.flushExaminerDigests(Instant.now()));

        verify(sender, never()).send(any(), any());
        verify(repository).addPendingExaminerJob(eq(jobs.get(0)), eq(examiner), any());
        verify(repository).addPendingExaminerJob(eq(jobs.get(1)), eq(examiner), any());
    }

    @Test
    void examinerDigestDropsTheJobsWhenTheExaminerSwitchedMailOff() {
        UUID examiner = UUID.randomUUID();
        when(repository.examinersWithPendingJobs()).thenReturn(List.of(examiner));
        when(repository.claimPendingExaminerJobs(examiner)).thenReturn(List.of(UUID.randomUUID()));
        when(repository.findContact(examiner)).thenReturn(Optional.of(new LearningMailRepository.Contact("ex@example.test", "Cô Lan")));
        when(repository.learningEnabled(examiner)).thenReturn(false);

        assertEquals(0, useCase.flushExaminerDigests(Instant.now()));

        verify(repository, never()).addPendingExaminerJob(any(), any(), any());
    }

    @Test
    void mailThatLeftIsNotRetriedWhenItsRecordFails() {
        UUID learner = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(deliveries.exists(eventId, "essay-graded")).thenReturn(false);
        when(repository.findContact(learner)).thenReturn(Optional.of(new LearningMailRepository.Contact("learner@example.test", "An")));
        org.mockito.Mockito.doThrow(new IllegalStateException("db down")).when(deliveries).recordSent(any(), any(), any());

        useCase.gradingCompleted(eventId, learner);

        verify(sender).send(eq("learner@example.test"), eq(RENDERED));
        verify(deliveries, never()).releaseDailyNonSecuritySlot(any(), any(), any(), any());
    }

    @Test
    void learnerWhoSwitchedLearningMailOffGetsNothingButTheEventIsSettled() {
        UUID learner = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(deliveries.exists(eventId, "essay-graded")).thenReturn(false);
        when(repository.findContact(learner)).thenReturn(Optional.of(new LearningMailRepository.Contact("learner@example.test", "An")));
        when(repository.learningEnabled(learner)).thenReturn(false);

        useCase.gradingCompleted(eventId, learner);

        verify(sender, never()).send(any(), any());
        verify(deliveries, never()).recordSent(any(), any(), any());
    }

    @Test
    void learnerMailCarriesAnUnsubscribeLinkWithASignedToken() {
        UUID learner = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(deliveries.exists(eventId, "essay-graded")).thenReturn(false);
        when(repository.findContact(learner)).thenReturn(Optional.of(new LearningMailRepository.Contact("learner@example.test", "An")));

        useCase.gradingCompleted(eventId, learner);

        verify(renderer).render(eq(MailTemplate.ESSAY_GRADED), argThatUnsubscribeStartsWith("https://api.example.test/mail/unsubscribe?token="));
        verify(sender).send(eq("learner@example.test"), eq(RENDERED));
        verify(deliveries).recordSent(eq(eventId), eq("essay-graded"), any());
    }

    @Test
    void missingContactSkipsTheMailWithoutFailingTheEvent() {
        UUID learner = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(deliveries.exists(eventId, "essay-graded")).thenReturn(false);
        when(repository.findContact(learner)).thenReturn(Optional.empty());

        useCase.gradingCompleted(eventId, learner);

        verify(sender, never()).send(any(), any());
    }

    @Test
    void learningMailStopsWhenThreeDailySlotsAreAlreadyReserved() {
        UUID learner = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(deliveries.exists(eventId, "essay-graded")).thenReturn(false);
        when(repository.findContact(learner)).thenReturn(Optional.of(new LearningMailRepository.Contact("learner@example.test", "An")));
        when(deliveries.reserveDailyNonSecuritySlot(eq(learner), any(LocalDate.class), eq(eventId),
                eq("essay-graded"), any())).thenReturn(false);

        useCase.gradingCompleted(eventId, learner);

        verify(sender, never()).send(any(), any());
        verify(deliveries, never()).recordSent(any(), any(), any());
    }

    @Test
    void billingNoticeIgnoresTheLearningSwitch() {
        UUID learner = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(deliveries.exists(eventId, "subscription-granted")).thenReturn(false);
        when(repository.findContact(learner)).thenReturn(Optional.of(new LearningMailRepository.Contact("learner@example.test", "An")));
        when(repository.learningEnabled(learner)).thenReturn(false);

        useCase.subscriptionGranted(eventId, learner, Instant.now().plus(30, ChronoUnit.DAYS));

        verify(sender).send(eq("learner@example.test"), eq(RENDERED));
    }

    @Test
    void unsubscribeTokensRoundTripAndRejectForgeryAndExpiry() {
        MailUnsubscribeTokens tokens = new MailUnsubscribeTokens(SECRET);
        UUID user = UUID.randomUUID();
        Instant now = Instant.now();
        String token = tokens.issue(user, "learning", now);

        assertEquals(user, tokens.verify(token, now).orElseThrow().userId());
        assertEquals("learning", tokens.verify(token, now).orElseThrow().category());
        assertFalse(tokens.verify(token + "x", now).isPresent());
        assertFalse(new MailUnsubscribeTokens("another-secret-value").verify(token, now).isPresent());
        assertFalse(tokens.verify(token, now.plus(61, ChronoUnit.DAYS)).isPresent());
        assertTrue(tokens.verify(token, now.plus(59, ChronoUnit.DAYS)).isPresent());
    }

    private static Map<String, Object> argThatUnsubscribeStartsWith(String prefix) {
        return org.mockito.ArgumentMatchers.argThat(model ->
                String.valueOf(model.get("unsubscribeLink")).startsWith(prefix));
    }
}
