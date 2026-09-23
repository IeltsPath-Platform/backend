package com.group01.access.domain.aggregate;

import com.group01.access.domain.vo.KeyType;
import com.group01.access.domain.vo.PlanStatus;

import java.time.Instant;
import java.util.UUID;

public class KeyProduct {

    private final UUID id;
    private final String code;
    private String name;
    private final KeyType keyType;
    private Integer pointsAmount;
    private UUID planId;
    private Integer premiumDays;
    private Integer humanGradingCredits;
    private PlanStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public KeyProduct(UUID id, String code, String name, KeyType keyType, Integer pointsAmount, UUID planId, Integer premiumDays, Integer humanGradingCredits, PlanStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.keyType = keyType;
        this.pointsAmount = pointsAmount;
        this.planId = planId;
        this.premiumDays = premiumDays;
        this.humanGradingCredits = humanGradingCredits;
        this.status = status;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static KeyProduct createPointsProduct(String code, String name, int pointsAmount) {
        if (pointsAmount <= 0) {
            throw new IllegalArgumentException("Points amount must be greater than 0");
        }
        Instant now = Instant.now();
        return new KeyProduct(UUID.randomUUID(), code, name, KeyType.POINTS, pointsAmount, null, null, null, PlanStatus.ACTIVE, now, now);
    }

    public static KeyProduct createPremiumProduct(String code, String name, UUID planId, int premiumDays, int humanGradingCredits) {
        if (premiumDays <= 0) {
            throw new IllegalArgumentException("Premium days must be greater than 0");
        }
        if (humanGradingCredits <= 0) {
            throw new IllegalArgumentException("Human grading credits must be greater than 0");
        }
        Instant now = Instant.now();
        return new KeyProduct(UUID.randomUUID(), code, name, KeyType.PREMIUM, null, planId, premiumDays, humanGradingCredits, PlanStatus.ACTIVE, now, now);
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public KeyType getKeyType() {
        return keyType;
    }

    public Integer getPointsAmount() {
        return pointsAmount;
    }

    public UUID getPlanId() {
        return planId;
    }

    public Integer getPremiumDays() {
        return premiumDays;
    }

    public Integer getHumanGradingCredits() {
        return humanGradingCredits;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isPointsProduct() {
        return keyType == KeyType.POINTS;
    }

    public boolean isPremiumProduct() {
        return keyType == KeyType.PREMIUM;
    }

    public void update(String name, PlanStatus status, Integer pointsAmount, UUID planId, Integer premiumDays, Integer humanGradingCredits) {
        this.name = name;
        this.status = status;
        this.pointsAmount = pointsAmount;
        this.planId = planId;
        this.premiumDays = premiumDays;
        this.humanGradingCredits = humanGradingCredits;
        this.updatedAt = Instant.now();
    }
}
