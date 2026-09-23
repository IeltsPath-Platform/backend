package com.group01.access.application.usecase;

import com.group01.access.application.command.ConsumeHumanGradingCreditCommand;
import com.group01.access.domain.aggregate.Subscription;
import com.group01.access.domain.entity.OutboxEvent;
import com.group01.access.domain.exception.InsufficientCreditsException;
import com.group01.access.domain.repository.OutboxEventRepository;
import com.group01.access.domain.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConsumeHumanGradingCreditUseCase {

    private final SubscriptionRepository subscriptionRepository;
    private final OutboxEventRepository outboxEventRepository;

    public ConsumeHumanGradingCreditUseCase(
            SubscriptionRepository subscriptionRepository,
            OutboxEventRepository outboxEventRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    public int execute(ConsumeHumanGradingCreditCommand command) {
        Subscription subscription = subscriptionRepository.findActiveByUserId(command.userId())
                .orElseThrow(() -> new InsufficientCreditsException(null, 0));

        subscription.consumeHumanGradingCredit();
        subscriptionRepository.save(subscription);

        outboxEventRepository.save(OutboxEvent.create(
                "Subscription", subscription.getId(), "HumanGradingCreditConsumed",
                String.format("{\"userId\":\"%s\",\"subscriptionId\":\"%s\",\"remainingCredits\":%d}",
                        command.userId(), subscription.getId(), subscription.getRemainingCredits())
        ));

        return subscription.getRemainingCredits();
    }
}
