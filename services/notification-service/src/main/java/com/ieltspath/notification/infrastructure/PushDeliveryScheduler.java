package com.ieltspath.notification.infrastructure;

import com.ieltspath.notification.application.ProcessPushDeliveryUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notification.push.worker-enabled", havingValue = "true")
public class PushDeliveryScheduler {
    private static final Logger log = LoggerFactory.getLogger(PushDeliveryScheduler.class);
    private final ProcessPushDeliveryUseCase deliveries;

    public PushDeliveryScheduler(ProcessPushDeliveryUseCase deliveries) {
        this.deliveries = deliveries;
    }

    @Scheduled(fixedDelayString = "${notification.push.poll-interval:PT1S}")
    public void dispatch() {
        try {
            deliveries.processBatch();
        } catch (RuntimeException failure) {
            log.warn("FCM delivery batch failed: errorType={}", failure.getClass().getSimpleName());
        }
    }
}
