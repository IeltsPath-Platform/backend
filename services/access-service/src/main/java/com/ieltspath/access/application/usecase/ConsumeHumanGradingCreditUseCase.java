package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.command.ConsumeHumanGradingCreditCommand;
import com.ieltspath.access.application.command.RefundHumanGradingCreditCommand;
import com.ieltspath.access.domain.aggregate.Subscription;
import com.ieltspath.access.domain.entity.HumanGradingCreditConsumption;
import com.ieltspath.access.domain.entity.OutboxEvent;
import com.ieltspath.access.domain.repository.PlanRepository;
import com.ieltspath.access.domain.exception.DuplicateIdempotencyException;
import com.ieltspath.access.domain.exception.InsufficientCreditsException;
import com.ieltspath.access.domain.repository.HumanGradingCreditConsumptionRepository;
import com.ieltspath.access.domain.repository.OutboxEventRepository;
import com.ieltspath.access.domain.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConsumeHumanGradingCreditUseCase {

    private final SubscriptionRepository subscriptionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final HumanGradingCreditConsumptionRepository consumptionRepository;
    private final PlanRepository planRepository;

    public ConsumeHumanGradingCreditUseCase(
            SubscriptionRepository subscriptionRepository,
            OutboxEventRepository outboxEventRepository,
            HumanGradingCreditConsumptionRepository consumptionRepository,
            PlanRepository planRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.consumptionRepository = consumptionRepository;
        this.planRepository = planRepository;
    }

    public int execute(ConsumeHumanGradingCreditCommand command) {
        return consume(command).remainingCredits();
    }

    /** What a consume call did: remaining credits and whether this call spent one. */
    public record Consumption(int remainingCredits, boolean charged) {}

    public Consumption consume(ConsumeHumanGradingCreditCommand command) {
        if (command.submissionId() != null) {
            return consumeOnce(command);
        }
        return consumeUntracked(command.userId());
    }

    private Consumption consumeOnce(ConsumeHumanGradingCreditCommand command) {
        consumptionRepository.lockSubmission(command.submissionId());
        var existing = consumptionRepository.findBySubmissionId(command.submissionId());
        if (existing.isPresent()) {
            var consumption = existing.get();
            requireSameUser(command.userId(), consumption.userId());
            if (consumption.refundedAt() == null) {
                return new Consumption(consumption.remainingCredits(), false);
            }
            // The earlier credit was refunded, so this attempt is a new request and costs a credit again.
            Debit debit = debit(command.userId());
            Subscription subscription = debit.subscription();
            int remaining = subscription.getRemainingCredits();
            consumptionRepository.markReconsumed(command.submissionId(), subscription.getId(), remaining, debit.creditSpent());
            recordConsumed(subscription, command, remaining);
            return new Consumption(remaining, debit.creditSpent());
        }
        Debit debit = debit(command.userId());
        Subscription subscription = debit.subscription();
        int remaining = subscription.getRemainingCredits();
        consumptionRepository.save(new HumanGradingCreditConsumption(command.submissionId(), command.userId(),
                subscription.getId(), remaining, null, debit.creditSpent()));
        recordConsumed(subscription, command, remaining);
        return new Consumption(remaining, debit.creditSpent());
    }

    private Debit debit(java.util.UUID userId) {
        Subscription subscription = subscriptionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new InsufficientCreditsException(null, 0));
        boolean unlimited = planRepository.findById(subscription.getPlanId())
                .map(plan -> plan.hasFeature("HUMAN_GRADING_UNLIMITED"))
                .orElse(false);
        if (!unlimited) {
            subscription.consumeHumanGradingCredit();
            subscriptionRepository.save(subscription);
        }
        return new Debit(subscription, !unlimited);
    }

    private record Debit(Subscription subscription, boolean creditSpent) {}

    private void recordConsumed(Subscription subscription, ConsumeHumanGradingCreditCommand command, int remaining) {
        outboxEventRepository.save(OutboxEvent.create(
                "Subscription", subscription.getId(), "HumanGradingCreditConsumed",
                String.format("{\"userId\":\"%s\",\"subscriptionId\":\"%s\",\"submissionId\":\"%s\",\"remainingCredits\":%d}",
                        command.userId(), subscription.getId(), command.submissionId(), remaining)
        ));
    }

    /** A credit that is not tied to a submission: spent every time, with nothing to make a repeat idempotent. */
    private Consumption consumeUntracked(java.util.UUID userId) {
        Debit debit = debit(userId);
        Subscription subscription = debit.subscription();
        outboxEventRepository.save(OutboxEvent.create(
                "Subscription", subscription.getId(), "HumanGradingCreditConsumed",
                String.format("{\"userId\":\"%s\",\"subscriptionId\":\"%s\",\"remainingCredits\":%d}",
                        userId, subscription.getId(), subscription.getRemainingCredits())
        ));
        return new Consumption(subscription.getRemainingCredits(), debit.creditSpent());
    }

    public int refund(RefundHumanGradingCreditCommand command) {
        consumptionRepository.lockSubmission(command.submissionId());
        var consumption = consumptionRepository.findBySubmissionId(command.submissionId())
                .orElseThrow(() -> new IllegalArgumentException("No human grading credit was consumed for this submission"));
        requireSameUser(command.userId(), consumption.userId());
        if (consumption.refundedAt() != null) {
            return consumption.remainingCredits();
        }
        Subscription subscription = subscriptionRepository.findById(consumption.subscriptionId())
                .orElseThrow(() -> new IllegalStateException("Consumed subscription no longer exists"));
        if (consumption.creditSpent()) {
            subscription.refundHumanGradingCredit();
            subscriptionRepository.save(subscription);
        }
        int remaining = subscription.getRemainingCredits();
        consumptionRepository.markRefunded(command.submissionId(), remaining);
        outboxEventRepository.save(OutboxEvent.create(
                "Subscription", subscription.getId(), "HumanGradingCreditRefunded",
                String.format("{\"userId\":\"%s\",\"subscriptionId\":\"%s\",\"submissionId\":\"%s\",\"remainingCredits\":%d}",
                        command.userId(), subscription.getId(), command.submissionId(), remaining)
        ));
        return remaining;
    }

    private static void requireSameUser(java.util.UUID requested, java.util.UUID actual) {
        if (!requested.equals(actual)) {
            throw new DuplicateIdempotencyException("Submission id was already used by another user");
        }
    }
}
