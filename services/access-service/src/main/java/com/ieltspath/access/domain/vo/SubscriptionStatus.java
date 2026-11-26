package com.ieltspath.access.domain.vo;

public enum SubscriptionStatus {
    ACTIVE,
    EXPIRED,
    CANCELLED;

    public boolean canMoveTo(SubscriptionStatus next) {
        return this == ACTIVE && (next == EXPIRED || next == CANCELLED);
    }
}
