package com.ieltspath.notification.domain.repository;

import com.ieltspath.notification.domain.vo.PushDelivery;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PushDeliveryRepository {
    /**
     * Claims up to {@code limit} due deliveries, counts the attempt and hides them for {@code lease} so another instance
     * does not send them meanwhile. The claim is committed before return; a crash mid-send only delays the retry.
     */
    List<PushDelivery> claimDue(int limit, Duration lease);

    void markSent(UUID id, String providerMessageId);

    void retryAt(UUID id, Instant nextAttemptAt, String error);

    void markFailed(UUID id, String error);
}
