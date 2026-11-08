package com.ieltspath.notification.domain.repository;

import com.ieltspath.notification.domain.vo.Notification;
import com.ieltspath.notification.domain.vo.PushDeliveryJob;
import com.ieltspath.notification.domain.vo.PushSendSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PushDeliveryRepository {
    int enqueueFor(Notification notification);
    List<PushDeliveryJob> claimDue(Instant now, int batchSize, Instant leaseUntil, int maxAttempts);
    PushSendSnapshot loadCurrentBinding(PushDeliveryJob job);
    boolean markSent(PushDeliveryJob job, Instant now, String providerMessageId);
    boolean scheduleRetry(PushDeliveryJob job, Instant now, Instant nextAttemptAt, String errorCode);
    boolean park(PushDeliveryJob job, Instant now, String errorCode);
    boolean cancel(PushDeliveryJob job, Instant now, String reason);
    boolean disableInvalidBindingAndCancel(PushDeliveryJob job, String tokenHash, Instant now);
    long countPending();
    Instant oldestPendingAt();
}
