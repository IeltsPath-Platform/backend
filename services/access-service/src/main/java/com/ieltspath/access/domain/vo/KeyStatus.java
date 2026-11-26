package com.ieltspath.access.domain.vo;

public enum KeyStatus {
    ACTIVE,
    REDEEMED,
    REVOKED,
    EXPIRED;

    public boolean canMoveTo(KeyStatus next) {
        return this == ACTIVE && (next == REDEEMED || next == REVOKED || next == EXPIRED);
    }
}
