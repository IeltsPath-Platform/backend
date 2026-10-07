package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.command.GrantSubscriptionCommand;
import com.ieltspath.access.application.result.SubscriptionResult;
import com.ieltspath.access.domain.aggregate.Plan;
import com.ieltspath.access.domain.aggregate.Subscription;
import com.ieltspath.access.domain.entity.OutboxEvent;
import com.ieltspath.access.domain.exception.PlanNotFoundException;
import com.ieltspath.access.domain.repository.OutboxEventRepository;
import com.ieltspath.access.domain.repository.PlanRepository;
import com.ieltspath.access.domain.repository.SubscriptionRepository;
import com.ieltspath.access.domain.vo.SubscriptionSourceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class GrantSubscriptionUseCase {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final OutboxEventRepository outboxEventRepository;

    public GrantSubscriptionUseCase(
            SubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            OutboxEventRepository outboxEventRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    public SubscriptionResult execute(GrantSubscriptionCommand command) {
        Plan plan = planRepository.findById(command.planId())
                .orElseThrow(() -> new PlanNotFoundException(command.planId()));

        Optional<Subscription> activeSub = subscriptionRepository.findActiveByUserId(command.userId());
        Subscription subscription;
        if (activeSub.isPresent()) {
            subscription = activeSub.get();
            subscription.extend(command.durationDays(), command.humanGradingCredits());
        } else {
            subscription = Subscription.create(
                    command.userId(),
                    command.planId(),
                    SubscriptionSourceType.ADMIN,
                    UUID.randomUUID(),
                    command.durationDays(),
                    command.humanGradingCredits()
            );
        }
        Subscription saved = subscriptionRepository.save(subscription);

        outboxEventRepository.save(OutboxEvent.create(
                "Subscription", saved.getId(), "SubscriptionGranted",
                String.format("{\"userId\":\"%s\",\"subscriptionId\":\"%s\",\"endsAt\":\"%s\"}",
                        command.userId(), saved.getId(), saved.getEndsAt())
        ));

        return new SubscriptionResult(
                saved.getId(),
                saved.getUserId(),
                saved.getPlanId(),
                plan.getCode(),
                plan.getName(),
                saved.getStatus(),
                saved.getStartsAt(),
                saved.getEndsAt(),
                saved.getHumanGradingCreditsTotal(),
                saved.getHumanGradingCreditsUsed(),
                saved.getRemainingCredits(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }
}
