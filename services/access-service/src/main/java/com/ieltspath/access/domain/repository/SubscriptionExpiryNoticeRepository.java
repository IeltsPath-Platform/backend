package com.ieltspath.access.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Subscriptions nearing or past their end, and the record of which expiry stages were already claimed. */
public interface SubscriptionExpiryNoticeRepository {
    /**
     * Subscriptions ending after {@code endedAfter} and at or before {@code endingBefore} whose final "expired" stage has
     * not been claimed for that end date: active ones, and ones already marked expired whose learner has no active
     * subscription now and none that ended later. The second group is needed because another job marks lapsed
     * subscriptions as expired.
     */
    List<DueSubscription> findDueForNotice(Instant endingBefore, Instant endedAfter);

    /**
     * Claims the stage of the subscription for that end date. Returns false when it was claimed before, so each stage is
     * emitted at most once per end date; a renewed or extended subscription has a new end date.
     */
    boolean claim(UUID subscriptionId, String stage, Instant endsAt, Instant claimedAt);

    record DueSubscription(UUID subscriptionId, UUID userId, Instant endsAt) { }
}
