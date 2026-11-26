package com.ieltspath.access.infrastructure.scheduler;

import com.ieltspath.access.application.usecase.ExpireLapsedSubscriptionsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SubscriptionLapseScheduler {
    private final ExpireLapsedSubscriptionsUseCase useCase;

    @Scheduled(fixedDelayString = "${access.subscription-lapse.scan-interval:PT15M}")
    public void scan() {
        useCase.execute(Instant.now());
    }
}
