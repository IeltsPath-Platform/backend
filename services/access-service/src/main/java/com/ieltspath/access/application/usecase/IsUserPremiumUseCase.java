package com.ieltspath.access.application.usecase;

import com.ieltspath.access.domain.aggregate.Subscription;
import com.ieltspath.access.domain.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class IsUserPremiumUseCase {
    private final SubscriptionRepository subscriptionRepository;

    public IsUserPremiumUseCase(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public boolean execute(UUID userId) {
        return subscriptionRepository.findActiveByUserId(userId)
                .map(Subscription::isActive)
                .orElse(false);
    }
}
