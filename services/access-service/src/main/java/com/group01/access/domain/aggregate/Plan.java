package com.group01.access.domain.aggregate;

import com.group01.access.domain.entity.PlanFeature;
import com.group01.access.domain.vo.PlanStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Plan {

    private final UUID id;
    private final String code;
    private String name;
    private PlanStatus status;
    private final List<PlanFeature> features;
    private final Instant createdAt;
    private Instant updatedAt;

    public Plan(UUID id, String code, String name, PlanStatus status, List<PlanFeature> features, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.status = status;
        this.features = features != null ? new ArrayList<>(features) : new ArrayList<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Plan create(String code, String name) {
        Instant now = Instant.now();
        return new Plan(UUID.randomUUID(), code, name, PlanStatus.ACTIVE, new ArrayList<>(), now, now);
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

    public PlanStatus getStatus() {
        return status;
    }

    public List<PlanFeature> getFeatures() {
        return Collections.unmodifiableList(features);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(String name, PlanStatus status) {
        this.name = name;
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void addFeature(PlanFeature feature) {
        this.features.removeIf(f -> f.getFeatureKey().equalsIgnoreCase(feature.getFeatureKey()));
        this.features.add(feature);
        this.updatedAt = Instant.now();
    }

    public boolean hasFeature(String featureKey) {
        return features.stream()
                .anyMatch(f -> f.getFeatureKey().equalsIgnoreCase(featureKey) && f.isEnabled());
    }
}
