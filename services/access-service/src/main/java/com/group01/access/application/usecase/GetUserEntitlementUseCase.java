package com.group01.access.application.usecase;

import com.group01.access.application.result.UserEntitlementResult;
import com.group01.access.domain.aggregate.Plan;
import com.group01.access.domain.aggregate.PointWallet;
import com.group01.access.domain.aggregate.Subscription;
import com.group01.access.domain.entity.PlanFeature;
import com.group01.access.domain.repository.PlanRepository;
import com.group01.access.domain.repository.PointWalletRepository;
import com.group01.access.domain.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetUserEntitlementUseCase {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final PointWalletRepository pointWalletRepository;

    public GetUserEntitlementUseCase(
            SubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            PointWalletRepository pointWalletRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.pointWalletRepository = pointWalletRepository;
    }

    public UserEntitlementResult execute(UUID userId) {
        Optional<Subscription> activeSub = subscriptionRepository.findActiveByUserId(userId);
        boolean isPremium = activeSub.isPresent() && activeSub.get().isActive();
        Instant premiumEndsAt = isPremium ? activeSub.get().getEndsAt() : null;
        int remainingCredits = isPremium ? activeSub.get().getRemainingCredits() : 0;

        List<String> features = Collections.emptyList();
        if (isPremium) {
            features = planRepository.findById(activeSub.get().getPlanId())
                    .map(Plan::getFeatures)
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(PlanFeature::isEnabled)
                    .map(PlanFeature::getFeatureKey)
                    .toList();
        }

        long pointBalance = pointWalletRepository.findByUserId(userId)
                .map(PointWallet::getBalance)
                .orElse(0L);

        return new UserEntitlementResult(
                userId,
                isPremium,
                premiumEndsAt,
                remainingCredits,
                pointBalance,
                features
        );
    }
}
