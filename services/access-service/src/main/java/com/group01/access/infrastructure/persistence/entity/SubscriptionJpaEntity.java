package com.group01.access.infrastructure.persistence.entity;

import com.group01.access.domain.vo.SubscriptionSourceType;
import com.group01.access.domain.vo.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SubscriptionStatus status;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private SubscriptionSourceType sourceType;

    @Column(name = "source_reference_id")
    private UUID sourceReferenceId;

    @Column(name = "human_grading_credits_total", nullable = false)
    private int humanGradingCreditsTotal;

    @Column(name = "human_grading_credits_used", nullable = false)
    private int humanGradingCreditsUsed;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
