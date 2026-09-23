package com.group01.access.domain.entity;

import com.group01.access.domain.vo.PointTransactionType;

import java.time.Instant;
import java.util.UUID;

public class PointLedgerEntry {

    private final UUID id;
    private final UUID userId;
    private final long delta;
    private final long balanceAfter;
    private final PointTransactionType transactionType;
    private final String referenceType;
    private final UUID referenceId;
    private final String idempotencyKey;
    private final String description;
    private final Instant createdAt;

    public PointLedgerEntry(UUID id, UUID userId, long delta, long balanceAfter, PointTransactionType transactionType, String referenceType, UUID referenceId, String idempotencyKey, String description, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.delta = delta;
        this.balanceAfter = balanceAfter;
        this.transactionType = transactionType;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.idempotencyKey = idempotencyKey;
        this.description = description;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static PointLedgerEntry create(UUID userId, long delta, long balanceAfter, PointTransactionType transactionType, String referenceType, UUID referenceId, String idempotencyKey, String description) {
        return new PointLedgerEntry(UUID.randomUUID(), userId, delta, balanceAfter, transactionType, referenceType, referenceId, idempotencyKey, description, Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public long getDelta() {
        return delta;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public PointTransactionType getTransactionType() {
        return transactionType;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
