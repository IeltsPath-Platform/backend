package com.group01.access.domain.entity;

import com.group01.access.domain.vo.KeyType;

import java.time.Instant;
import java.util.UUID;

public class KeyActivation {

    private final UUID id;
    private final UUID keyId;
    private final UUID userId;
    private final KeyType productType;
    private final int pointsGranted;
    private final int premiumDaysGranted;
    private final int humanGradingCreditsGranted;
    private final Instant activatedAt;
    private final String idempotencyKey;

    public KeyActivation(UUID id, UUID keyId, UUID userId, KeyType productType, int pointsGranted, int premiumDaysGranted, int humanGradingCreditsGranted, Instant activatedAt, String idempotencyKey) {
        this.id = id;
        this.keyId = keyId;
        this.userId = userId;
        this.productType = productType;
        this.pointsGranted = pointsGranted;
        this.premiumDaysGranted = premiumDaysGranted;
        this.humanGradingCreditsGranted = humanGradingCreditsGranted;
        this.activatedAt = activatedAt != null ? activatedAt : Instant.now();
        this.idempotencyKey = idempotencyKey;
    }

    public static KeyActivation create(UUID keyId, UUID userId, KeyType productType, int pointsGranted, int premiumDaysGranted, int humanGradingCreditsGranted, String idempotencyKey) {
        return new KeyActivation(UUID.randomUUID(), keyId, userId, productType, pointsGranted, premiumDaysGranted, humanGradingCreditsGranted, Instant.now(), idempotencyKey);
    }

    public UUID getId() {
        return id;
    }

    public UUID getKeyId() {
        return keyId;
    }

    public UUID getUserId() {
        return userId;
    }

    public KeyType getProductType() {
        return productType;
    }

    public int getPointsGranted() {
        return pointsGranted;
    }

    public int getPremiumDaysGranted() {
        return premiumDaysGranted;
    }

    public int getHumanGradingCreditsGranted() {
        return humanGradingCreditsGranted;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
