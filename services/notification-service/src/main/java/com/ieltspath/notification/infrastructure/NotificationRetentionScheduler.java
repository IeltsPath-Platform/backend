package com.ieltspath.notification.infrastructure;

import com.ieltspath.notification.application.NotificationRetentionUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class NotificationRetentionScheduler {
    private final NotificationRetentionUseCase retention;
    @Value("${notification.retention.batch-size:500}") private int batchSize;

    public NotificationRetentionScheduler(NotificationRetentionUseCase retention) {
        this.retention = retention;
    }

    @Scheduled(fixedDelayString = "${notification.retention.cleanup-interval:PT6H}",
            initialDelayString = "${notification.retention.initial-delay:PT2M}")
    public void cleanup() {
        retention.deleteExpiredBatch(Instant.now(), batchSize);
    }
}
