package com.group01.access.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plan_features", uniqueConstraints = {
    @UniqueConstraint(name = "uq_plan_feature", columnNames = {"plan_id", "feature_key"})
})
@Getter
@Setter
@NoArgsConstructor
public class PlanFeatureJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanJpaEntity plan;

    @Column(name = "feature_key", nullable = false, length = 100)
    private String featureKey;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;

    @Column(name = "limit_value")
    private Integer limitValue;

    @Column(columnDefinition = "jsonb")
    private String config;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
