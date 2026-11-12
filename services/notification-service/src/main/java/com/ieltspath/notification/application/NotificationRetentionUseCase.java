package com.ieltspath.notification.application;

import com.ieltspath.notification.domain.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class NotificationRetentionUseCase {
    private final NotificationRepository notifications;

    public NotificationRetentionUseCase(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional
    public int deleteExpiredBatch(Instant now, int batchSize) {
        if (batchSize < 1 || batchSize > 10_000) throw new IllegalArgumentException("Invalid batch size");
        return notifications.deleteExpiredBatch(now.minus(90, ChronoUnit.DAYS), now.minus(180, ChronoUnit.DAYS), batchSize);
    }
}
