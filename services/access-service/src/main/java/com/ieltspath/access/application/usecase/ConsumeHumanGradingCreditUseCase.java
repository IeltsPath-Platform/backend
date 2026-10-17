package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.command.ConsumeHumanGradingCreditCommand;
import com.ieltspath.access.application.command.RefundHumanGradingCreditCommand;
import com.ieltspath.access.domain.aggregate.Subscription;
import com.ieltspath.access.domain.entity.HumanGradingCreditConsumption;
import com.ieltspath.access.domain.entity.OutboxEvent;
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

    public ConsumeHumanGradingCreditUseCase(
            SubscriptionRepository subscriptionRepository,
            OutboxEventRepository outboxEventRepository,
            HumanGradingCreditConsumptionRepository consumptionRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.consumptionRepository = consumptionRepository;
    }

    public int execute(ConsumeHumanGradingCreditCommand command) {
        if (command.submissionId() != null) {
            return consumeOnce(command);
        }
        return consume(command.userId(), null);
    }

    private int consumeOnce(ConsumeHumanGradingCreditCommand command) {
        consumptionRepository.lockSubmission(command.submissionId());
        var existing = consumptionRepository.findBySubmissionId(command.submissionId());
        if (existing.isPresent()) {
            var consumption = existing.get();
            requireSameUser(command.userId(), consumption.userId());
            return consumption.remainingCredits();
        }
        Subscription subscription = subscriptionRepository.findActiveByUserId(command.userId())
                .orElseThrow(() -> new InsufficientCreditsException(null, 0));
        subscription.consumeHumanGradingCredit();
        subscriptionRepository.save(subscription);
        int remaining = subscription.getRemainingCredits();
        consumptionRepository.save(new HumanGradingCreditConsumption(command.submissionId(), command.userId(),
                subscription.getId(), remaining, null));
        outboxEventRepository.save(OutboxEvent.create(
                "Subscription", subscription.getId(), "HumanGradingCreditConsumed",
                String.format("{\"userId\":\"%s\",\"subscriptionId\":\"%s\",\"submissionId\":\"%s\",\"remainingCredits\":%d}",
                        command.userId(), subscription.getId(), command.submissionId(), remaining)
        ));
        return remaining;
    }

    private int consume(java.util.UUID userId, java.util.UUID submissionId) {
        Subscription subscription = subscriptionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new InsufficientCreditsException(null, 0));

        subscription.consumeHumanGradingCredit();
        subscriptionRepository.save(subscription);

        outboxEventRepository.save(OutboxEvent.create(
                "Subscription", subscription.getId(), "HumanGradingCreditConsumed",
                String.format("{\"userId\":\"%s\",\"subscriptionId\":\"%s\",\"remainingCredits\":%d}",
                        userId, subscription.getId(), subscription.getRemainingCredits())
        ));

        return subscription.getRemainingCredits();
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
        subscription.refundHumanGradingCredit();
        subscriptionRepository.save(subscription);
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
