package com.group01.assessment.infrastructure.persistence.entity;

import com.group01.assessment.domain.vo.GradingJobStatus;
import com.group01.assessment.domain.vo.GradingMode;
import com.group01.assessment.domain.vo.Skill;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "grading_jobs", uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"))
@Getter
@Setter
@NoArgsConstructor
public class GradingJobJpaEntity {
    @Id
    private UUID id;
    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Skill skill;
    @Enumerated(EnumType.STRING)
    @Column(name = "grading_mode", nullable = false)
    private GradingMode gradingMode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GradingJobStatus status;
    @Column(name = "point_cost_snapshot")
    private Integer pointCostSnapshot;
    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "completed_at")
    private Instant completedAt;
}
