package com.ieltspath.access.infrastructure.scheduler;

import com.ieltspath.access.application.usecase.SubscriptionExpiryNoticeUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "access.subscription-expiry.enabled", havingValue = "true", matchIfMissing = true)
public class SubscriptionExpiryNoticeScheduler {
    private final SubscriptionExpiryNoticeUseCase useCase;

    public SubscriptionExpiryNoticeScheduler(SubscriptionExpiryNoticeUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(fixedDelayString = "${access.subscription-expiry.scan-interval:PT15M}",
            initialDelayString = "${access.subscription-expiry.initial-delay:PT1M}")
    public void scan() {
        useCase.scan(Instant.now());
    }
}
