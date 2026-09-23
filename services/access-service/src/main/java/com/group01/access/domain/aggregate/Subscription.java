package com.group01.access.domain.aggregate;

import com.group01.access.domain.exception.InsufficientCreditsException;
import com.group01.access.domain.vo.SubscriptionSourceType;
import com.group01.access.domain.vo.SubscriptionStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class Subscription {

    private final UUID id;
    private final UUID userId;
    private final UUID planId;
    private SubscriptionStatus status;
    private final Instant startsAt;
    private Instant endsAt;
    private final SubscriptionSourceType sourceType;
    private final UUID sourceReferenceId;
    private int humanGradingCreditsTotal;
    private int humanGradingCreditsUsed;
    private Instant cancelledAt;
    private long rowVersion;
    private final Instant createdAt;
    private Instant updatedAt;

    public Subscription(UUID id, UUID userId, UUID planId, SubscriptionStatus status, Instant startsAt, Instant endsAt, SubscriptionSourceType sourceType, UUID sourceReferenceId, int humanGradingCreditsTotal, int humanGradingCreditsUsed, Instant cancelledAt, long rowVersion, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.planId = planId;
        this.status = status;
        this.startsAt = startsAt != null ? startsAt : Instant.now();
        this.endsAt = endsAt;
        this.sourceType = sourceType;
        this.sourceReferenceId = sourceReferenceId;
        this.humanGradingCreditsTotal = humanGradingCreditsTotal;
        this.humanGradingCreditsUsed = humanGradingCreditsUsed;
        this.cancelledAt = cancelledAt;
        this.rowVersion = rowVersion;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Subscription create(UUID userId, UUID planId, SubscriptionSourceType sourceType, UUID sourceReferenceId, int durationDays, int humanGradingCredits) {
        Instant now = Instant.now();
        Instant ends = now.plus(durationDays, ChronoUnit.DAYS);
        return new Subscription(UUID.randomUUID(), userId, planId, SubscriptionStatus.ACTIVE, now, ends, sourceType, sourceReferenceId, humanGradingCredits, 0, null, 0L, now, now);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getPlanId() {
        return planId;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public SubscriptionSourceType getSourceType() {
        return sourceType;
    }

    public UUID getSourceReferenceId() {
        return sourceReferenceId;
    }

    public int getHumanGradingCreditsTotal() {
        return humanGradingCreditsTotal;
    }

    public int getHumanGradingCreditsUsed() {
        return humanGradingCreditsUsed;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public long getRowVersion() {
        return rowVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE && (endsAt == null || endsAt.isAfter(Instant.now()));
    }

    public int getRemainingCredits() {
        return Math.max(0, humanGradingCreditsTotal - humanGradingCreditsUsed);
    }

    public void extend(int additionalDays, int additionalCredits) {
        Instant baseTime = (endsAt != null && endsAt.isAfter(Instant.now())) ? endsAt : Instant.now();
        this.endsAt = baseTime.plus(additionalDays, ChronoUnit.DAYS);
        this.humanGradingCreditsTotal += additionalCredits;
        this.status = SubscriptionStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void consumeHumanGradingCredit() {
        if (!isActive()) {
            throw new IllegalStateException("Cannot consume credit on inactive subscription");
        }
        if (getRemainingCredits() <= 0) {
            throw new InsufficientCreditsException(id, getRemainingCredits());
        }
        this.humanGradingCreditsUsed++;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
