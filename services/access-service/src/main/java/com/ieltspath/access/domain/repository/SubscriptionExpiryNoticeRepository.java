package com.ieltspath.access.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Active subscriptions nearing or past their end, and the record of which expiry stages were already claimed. */
public interface SubscriptionExpiryNoticeRepository {
    /** Active subscriptions ending at or before the cutoff whose final "expired" stage has not been claimed yet. */
    List<DueSubscription> findActiveEndingBefore(Instant cutoff);

    /** Claims the stage. Returns false when it was claimed before, so each stage is emitted at most once. */
    boolean claim(UUID subscriptionId, String stage, Instant claimedAt);

    record DueSubscription(UUID subscriptionId, UUID userId, Instant endsAt) { }
}
