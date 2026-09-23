package com.group01.access.infrastructure.persistence.entity;

import com.group01.access.domain.vo.KeyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "key_activations")
@Getter
@Setter
@NoArgsConstructor
public class KeyActivationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "key_id", nullable = false, unique = true)
    private UUID keyId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 50)
    private KeyType productType;

    @Column(name = "points_granted", nullable = false)
    private int pointsGranted;

    @Column(name = "premium_days_granted", nullable = false)
    private int premiumDaysGranted;

    @Column(name = "human_grading_credits_granted", nullable = false)
    private int humanGradingCreditsGranted;

    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;
}
