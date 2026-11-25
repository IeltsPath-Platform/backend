package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.command.ConsumeHumanGradingCreditCommand;
import com.ieltspath.access.application.command.RefundHumanGradingCreditCommand;
import com.ieltspath.access.domain.aggregate.Plan;
import com.ieltspath.access.domain.aggregate.Subscription;
import com.ieltspath.access.domain.exception.InsufficientCreditsException;
import com.ieltspath.access.domain.repository.HumanGradingCreditConsumptionRepository;
import com.ieltspath.access.domain.repository.OutboxEventRepository;
import com.ieltspath.access.domain.repository.PlanRepository;
import com.ieltspath.access.domain.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConsumeHumanGradingCreditUseCaseTest {
    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final OutboxEventRepository outbox = mock(OutboxEventRepository.class);
    private final HumanGradingCreditConsumptionRepository consumptions = mock(HumanGradingCreditConsumptionRepository.class);
    private final PlanRepository plans = mock(PlanRepository.class);
    private final ConsumeHumanGradingCreditUseCase useCase =
            new ConsumeHumanGradingCreditUseCase(subscriptions, outbox, consumptions, plans);

    @Test
    void unlimitedPlanDoesNotSpendAndRepeatIsIdempotent() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        Subscription subscription = mock(Subscription.class);
        Plan plan = mock(Plan.class);
        when(subscription.getId()).thenReturn(subscriptionId);
        when(subscription.getPlanId()).thenReturn(UUID.randomUUID());
        when(subscription.getRemainingCredits()).thenReturn(3);
        when(subscriptions.findActiveByUserId(userId)).thenReturn(Optional.of(subscription));
        when(plans.findById(subscription.getPlanId())).thenReturn(Optional.of(plan));
        when(plan.hasFeature("HUMAN_GRADING_UNLIMITED")).thenReturn(true);
        when(consumptions.findBySubmissionId(submissionId)).thenReturn(Optional.empty());

        var first = useCase.consume(new ConsumeHumanGradingCreditCommand(userId, submissionId));
        when(consumptions.findBySubmissionId(submissionId)).thenReturn(Optional.of(
                new com.ieltspath.access.domain.entity.HumanGradingCreditConsumption(submissionId, userId,
                        subscriptionId, 3, null, false)));
        var repeat = useCase.consume(new ConsumeHumanGradingCreditCommand(userId, submissionId));

        assertFalse(first.charged());
        assertFalse(repeat.charged());
        verify(subscription, never()).consumeHumanGradingCredit();
        verify(subscriptions, never()).save(subscription);
        verify(consumptions).save(argThat(saved -> !saved.creditSpent()));
    }

    @Test
    void refundOfUnlimitedRequestDoesNotMintCredit() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        Subscription subscription = mock(Subscription.class);
        when(subscription.getId()).thenReturn(subscriptionId);
        when(subscription.getRemainingCredits()).thenReturn(2);
        when(consumptions.findBySubmissionId(submissionId)).thenReturn(Optional.of(
                new com.ieltspath.access.domain.entity.HumanGradingCreditConsumption(submissionId, userId,
                        subscriptionId, 2, null, false)));
        when(subscriptions.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        assertEquals(2, useCase.refund(new RefundHumanGradingCreditCommand(userId, submissionId)));

        verify(subscription, never()).refundHumanGradingCredit();
        verify(subscriptions, never()).save(subscription);
        verify(consumptions).markRefunded(submissionId, 2);
    }

    @Test
    void normalPlanStillSpendsCreditAndMissingSubscriptionIsInsufficient() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        Subscription subscription = mock(Subscription.class);
        Plan plan = mock(Plan.class);
        when(subscription.getId()).thenReturn(subscriptionId);
        when(subscription.getPlanId()).thenReturn(UUID.randomUUID());
        when(subscription.getRemainingCredits()).thenReturn(1);
        when(subscriptions.findActiveByUserId(userId)).thenReturn(Optional.of(subscription));
        when(plans.findById(subscription.getPlanId())).thenReturn(Optional.of(plan));
        when(plan.hasFeature("HUMAN_GRADING_UNLIMITED")).thenReturn(false);
        when(consumptions.findBySubmissionId(submissionId)).thenReturn(Optional.empty());

        var result = useCase.consume(new ConsumeHumanGradingCreditCommand(userId, submissionId));

        assertTrue(result.charged());
        verify(subscription).consumeHumanGradingCredit();
        verify(subscriptions).save(subscription);

        when(subscriptions.findActiveByUserId(userId)).thenReturn(Optional.empty());
        assertThrows(InsufficientCreditsException.class,
                () -> useCase.consume(new ConsumeHumanGradingCreditCommand(userId, UUID.randomUUID())));
    }
}
