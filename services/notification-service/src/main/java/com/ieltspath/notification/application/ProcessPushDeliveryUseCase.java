package com.ieltspath.notification.application;

import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.notification.application.port.PushSendException;
import com.ieltspath.notification.application.port.PushTransientException;
import com.ieltspath.notification.application.port.PushSender;
import com.ieltspath.notification.domain.repository.PushDeliveryRepository;
import com.ieltspath.notification.domain.vo.PushDeliveryJob;
import com.ieltspath.notification.domain.vo.PushSendSnapshot;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ProcessPushDeliveryUseCase {
    private static final Logger log = LoggerFactory.getLogger(ProcessPushDeliveryUseCase.class);
    private static final int MAX_ATTEMPTS = 8;
    private static final Duration LEASE = Duration.ofSeconds(120);
    private static final int BATCH_SIZE = 2;

    private final PushDeliveryRepository deliveries;
    private final PushSender sender;
    private final ResilientCall resilient;
    private final MeterRegistry metrics;
    private final Clock clock;
    private final AtomicBoolean paused = new AtomicBoolean();
    private final AtomicBoolean warnedNotReady = new AtomicBoolean();

    @Autowired
    public ProcessPushDeliveryUseCase(PushDeliveryRepository deliveries, PushSender sender,
                                      ResilientCall resilient, MeterRegistry metrics) {
        this(deliveries, sender, resilient, metrics, Clock.systemUTC());
    }

    ProcessPushDeliveryUseCase(PushDeliveryRepository deliveries, PushSender sender,
                               ResilientCall resilient, MeterRegistry metrics, Clock clock) {
        this.deliveries = deliveries;
        this.sender = sender;
        this.resilient = resilient;
        this.metrics = metrics;
        this.clock = clock;
        Gauge.builder("notification_push_pending", deliveries, PushDeliveryRepository::countPending)
                .description("Push deliveries waiting for FCM")
                .register(metrics);
        Gauge.builder("notification_push_oldest_pending_age_seconds", deliveries, repository -> {
                    Instant oldest = repository.oldestPendingAt();
                    return oldest == null ? 0 : Math.max(0, Duration.between(oldest, this.clock.instant()).toSeconds());
                }).description("Age of the oldest pending push delivery").register(metrics);
    }

    public void processBatch() {
        if (paused.get()) return;
        if (!sender.isReady()) {
            if (warnedNotReady.compareAndSet(false, true))
                log.warn("FCM worker is paused because push credentials or project configuration are unavailable");
            return;
        }
        warnedNotReady.set(false);
        Instant now = clock.instant();
        for (PushDeliveryJob job : deliveries.claimDue(now, BATCH_SIZE, now.plus(LEASE), MAX_ATTEMPTS)) {
            if (paused.get()) {
                retry(job, new PushSendException(PushSendException.Category.RETRYABLE, "WORKER_PAUSED"), clock.instant());
                continue;
            }
            process(job);
        }
    }

    public boolean isPaused() { return paused.get(); }

    private void process(PushDeliveryJob job) {
        Instant now = clock.instant();
        PushSendSnapshot snapshot = deliveries.loadCurrentBinding(job);
        if (snapshot == null) {
            count("cancelled", "STALE_BINDING");
            deliveries.cancel(job, now, "STALE_BINDING");
            return;
        }
        try {
            String messageId = resilient.call("fcm", CallKind.WRITE, () -> sender.send(snapshot));
            if (deliveries.markSent(job, clock.instant(), messageId)) count("sent", "accepted");
        } catch (PushSendException failure) {
            switch (failure.category()) {
                case UNREGISTERED -> {
                    if (deliveries.disableInvalidBindingAndCancel(job, snapshot.tokenHash(), clock.instant()))
                        count("deactivated", failure.code());
                }
                case PERMANENT -> {
                    if (deliveries.park(job, clock.instant(), failure.code())) {
                        count("failed", failure.code());
                        count("parked", failure.code());
                    }
                }
                case CONFIGURATION -> {
                    paused.set(true);
                    log.error("FCM worker paused after provider configuration error: code={}", failure.code());
                    retry(job, failure, clock.instant());
                }
                case RETRYABLE -> retry(job, failure, clock.instant());
            }
        } catch (PushTransientException failure) {
            retry(job, new PushSendException(PushSendException.Category.RETRYABLE, failure.code(),
                    failure.retryAfterSeconds()), clock.instant());
        } catch (DownstreamUnavailableException unavailable) {
            retry(job, new PushSendException(PushSendException.Category.RETRYABLE, unavailable.reason()), clock.instant());
        } catch (RuntimeException failure) {
            log.warn("FCM delivery failed: deliveryId={}, errorType={}", job.id(), failure.getClass().getSimpleName());
            retry(job, new PushSendException(PushSendException.Category.RETRYABLE, "SEND_FAILED"), clock.instant());
        }
    }

    private void retry(PushDeliveryJob job, PushSendException failure, Instant now) {
        if (job.attemptCount() >= MAX_ATTEMPTS) {
            if (deliveries.park(job, now, failure.code())) count("parked", failure.code());
            return;
        }
        long base = Math.min(3600L, 30L << Math.min(job.attemptCount() - 1, 7));
        long jitter = ThreadLocalRandom.current().nextLong(Math.max(1, base / 2), base + 1);
        long delay = Math.max(jitter, failure.retryAfterSeconds());
        if ("QUOTA_EXCEEDED".equals(failure.code())) delay = Math.max(delay, 60);
        if (deliveries.scheduleRetry(job, now, now.plusSeconds(delay), failure.code())) count("retried", failure.code());
    }

    private void count(String outcome, String code) {
        String boundedCode = switch (code) {
            case "accepted", "STALE_BINDING", "UNREGISTERED", "QUOTA_EXCEEDED", "UNAVAILABLE", "INTERNAL",
                    "INVALID_ARGUMENT", "PERMISSION_DENIED", "UNAUTHENTICATED", "SEND_FAILED" -> code;
            default -> "other";
        };
        metrics.counter("notification_push_deliveries_total", "provider", "fcm", "outcome", outcome,
                "error", boundedCode).increment();
    }
}
