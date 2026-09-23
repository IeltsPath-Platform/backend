package com.group01.access.infrastructure.persistence.entity;

import com.group01.access.domain.vo.KeyType;
import com.group01.access.domain.vo.PlanStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "key_products")
@Getter
@Setter
@NoArgsConstructor
public class KeyProductJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", nullable = false, length = 50)
    private KeyType keyType;

    @Column(name = "points_amount")
    private Integer pointsAmount;

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "premium_days")
    private Integer premiumDays;

    @Column(name = "human_grading_credits")
    private Integer humanGradingCredits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PlanStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
