package com.ieltspath.access.application.usecase;

import com.ieltspath.access.domain.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ExpireLapsedSubscriptionsUseCase {
    private final SubscriptionRepository subscriptions;

    @Transactional
    public int execute(Instant now) {
        return subscriptions.expireAllLapsed(now);
    }
}
