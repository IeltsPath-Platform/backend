package com.ieltspath.notification.application;

import com.ieltspath.notification.domain.repository.PushDeliveryRepository;
import com.ieltspath.notification.domain.vo.Notification;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EnqueueNotificationPushUseCase {
    private final PushDeliveryRepository deliveries;
    private final boolean enabled;
    private final MeterRegistry metrics;

    public EnqueueNotificationPushUseCase(PushDeliveryRepository deliveries,
            @Value("${notification.push.enqueue-enabled:false}") boolean enabled, MeterRegistry metrics) {
        this.deliveries = deliveries;
        this.enabled = enabled;
        this.metrics = metrics;
    }

    private EnqueueNotificationPushUseCase() {
        this.deliveries = null;
        this.enabled = false;
        this.metrics = null;
    }

    public static EnqueueNotificationPushUseCase disabled() {
        return new EnqueueNotificationPushUseCase();
    }

    public int execute(Notification notification) {
        if (!enabled) return 0;
        int count = deliveries.enqueueFor(notification);
        if (count > 0) metrics.counter("notification_push_deliveries_total", "provider", "fcm",
                "outcome", "queued", "error", "none").increment(count);
        return count;
    }
}
