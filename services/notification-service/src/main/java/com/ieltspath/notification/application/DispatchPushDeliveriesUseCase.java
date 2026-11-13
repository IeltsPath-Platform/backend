package com.ieltspath.notification.application;

import com.ieltspath.notification.domain.repository.PushDeliveryRepository;
import com.ieltspath.notification.domain.repository.PushDeviceRepository;
import com.ieltspath.notification.domain.vo.PushDelivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sends due push deliveries. Each claim and each result is its own short write; the provider is called outside any
 * transaction or lock. A retry updates the delivery row only, so the learner never gets a second inbox notification.
 */
public class DispatchPushDeliveriesUseCase {
    public static final int BATCH = 50;
    static final int MAX_ATTEMPTS = 5;
    static final Duration LEASE = Duration.ofMinutes(2);
    static final Duration FIRST_BACKOFF = Duration.ofSeconds(30);
    private static final Logger log = LoggerFactory.getLogger(DispatchPushDeliveriesUseCase.class);

    private final PushDeliveryRepository deliveries;
    private final PushDeviceRepository devices;
    private final PushSender sender;
    private final Clock clock;

    public DispatchPushDeliveriesUseCase(PushDeliveryRepository deliveries, PushDeviceRepository devices, PushSender sender,
                                         Clock clock) {
        this.deliveries = deliveries;
        this.devices = devices;
        this.sender = sender;
        this.clock = clock;
    }

    /** @return how many deliveries were claimed */
    public int dispatchDue() {
        var due = deliveries.claimDue(BATCH, LEASE);
        due.forEach(this::dispatch);
        return due.size();
    }

    private void dispatch(PushDelivery delivery) {
        if (delivery.token() == null) {
            deliveries.markFailed(delivery.id(), "DEVICE_GONE");
            return;
        }
        var text = PushMessages.forType(delivery.eventType());
        try {
            deliveries.markSent(delivery.id(), sender.send(delivery.token(), text.title(), text.body(), data(delivery)));
            log.info("Push delivery sent: deliveryId={}", delivery.id());
        } catch (PushSender.PushSendException failure) {
            log.warn("Push delivery failed: deliveryId={} attempt={} errorType={}", delivery.id(), delivery.attempts(),
                    failure.code());
            if (failure.failure() == PushSender.Failure.TOKEN_INVALID) devices.deleteByToken(delivery.token());
            if (failure.failure() != PushSender.Failure.TRANSIENT || delivery.attempts() >= MAX_ATTEMPTS) {
                deliveries.markFailed(delivery.id(), failure.code());
            } else {
                Duration backoff = FIRST_BACKOFF.multipliedBy(1L << (delivery.attempts() - 1));
                deliveries.retryAt(delivery.id(), clock.instant().plus(backoff), failure.code());
            }
        }
    }

    /** Same ids as an inbox item, so the app can open the right screen without another request. */
    private static Map<String, String> data(PushDelivery d) {
        var data = new LinkedHashMap<String, String>();
        data.put("notificationId", d.notificationId().toString());
        data.put("eventType", d.eventType());
        if (d.postId() != null) data.put("postId", d.postId().toString());
        if (d.commentId() != null) data.put("commentId", d.commentId().toString());
        if (d.gradingJobId() != null) data.put("gradingJobId", d.gradingJobId().toString());
        return data;
    }
}
