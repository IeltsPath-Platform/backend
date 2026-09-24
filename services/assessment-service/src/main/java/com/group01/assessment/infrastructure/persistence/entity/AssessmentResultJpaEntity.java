package com.group01.assessment.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessment_results", uniqueConstraints = @UniqueConstraint(name = "uk_result_version", columnNames = {
        "attempt_id", "result_version"}))
@Getter
@Setter
@NoArgsConstructor
public class AssessmentResultJpaEntity {
    @Id
    private UUID id;
    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;
    @Column(name = "result_version", nullable = false)
    private int resultVersion;
    @Column(nullable = false)
    private String status;
    @Column(name = "overall_band", precision = 3, scale = 1)
    private BigDecimal overallBand;
    @Column(name = "completed_at")
    private Instant completedAt;
}
