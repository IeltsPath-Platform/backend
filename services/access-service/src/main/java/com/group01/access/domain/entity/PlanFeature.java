package com.group01.access.domain.entity;

import java.time.Instant;
import java.util.UUID;

public class PlanFeature {

    private final UUID id;
    private final UUID planId;
    private final String featureKey;
    private boolean enabled;
    private Integer limitValue;
    private String config;
    private final Instant createdAt;
    private Instant updatedAt;

    public PlanFeature(UUID id, UUID planId, String featureKey, boolean enabled, Integer limitValue, String config, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.planId = planId;
        this.featureKey = featureKey;
        this.enabled = enabled;
        this.limitValue = limitValue;
        this.config = config;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static PlanFeature create(UUID planId, String featureKey, boolean enabled, Integer limitValue, String config) {
        Instant now = Instant.now();
        return new PlanFeature(UUID.randomUUID(), planId, featureKey, enabled, limitValue, config, now, now);
    }

    public UUID getId() {
        return id;
    }

    public UUID getPlanId() {
        return planId;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Integer getLimitValue() {
        return limitValue;
    }

    public String getConfig() {
        return config;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(boolean enabled, Integer limitValue, String config) {
        this.enabled = enabled;
        this.limitValue = limitValue;
        this.config = config;
        this.updatedAt = Instant.now();
    }
}
