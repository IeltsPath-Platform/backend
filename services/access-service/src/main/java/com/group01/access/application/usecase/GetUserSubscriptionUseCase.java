package com.group01.access.application.usecase;

import com.group01.access.application.result.SubscriptionResult;
import com.group01.access.domain.aggregate.Plan;
import com.group01.access.domain.aggregate.Subscription;
import com.group01.access.domain.repository.PlanRepository;
import com.group01.access.domain.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetUserSubscriptionUseCase {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;

    public GetUserSubscriptionUseCase(SubscriptionRepository subscriptionRepository, PlanRepository planRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
    }

    public Optional<SubscriptionResult> execute(UUID userId) {
        return subscriptionRepository.findActiveByUserId(userId)
                .map(this::toResult);
    }

    private SubscriptionResult toResult(Subscription sub) {
        Plan plan = planRepository.findById(sub.getPlanId()).orElse(null);
        String planCode = plan != null ? plan.getCode() : "UNKNOWN";
        String planName = plan != null ? plan.getName() : "Unknown Plan";

        return new SubscriptionResult(
                sub.getId(),
                sub.getUserId(),
                sub.getPlanId(),
                planCode,
                planName,
                sub.getStatus(),
                sub.getStartsAt(),
                sub.getEndsAt(),
                sub.getHumanGradingCreditsTotal(),
                sub.getHumanGradingCreditsUsed(),
                sub.getRemainingCredits(),
                sub.getCreatedAt(),
                sub.getUpdatedAt()
        );
    }
}
