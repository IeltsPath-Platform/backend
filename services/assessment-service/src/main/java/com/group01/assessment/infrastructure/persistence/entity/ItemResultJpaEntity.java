package com.group01.assessment.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "item_results", uniqueConstraints = @UniqueConstraint(columnNames = {"result_id", "attempt_item_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ItemResultJpaEntity {
    @Id
    private UUID id;
    @Column(name = "result_id", nullable = false)
    private UUID resultId;
    @Column(name = "attempt_item_id", nullable = false)
    private UUID attemptItemId;
    @Column(name = "score", nullable = false, precision = 8, scale = 2)
    private BigDecimal score;
    @Column(name = "max_score", precision = 8, scale = 2)
    private BigDecimal maxScore;
    @Column(name = "is_correct")
    private Boolean correct;
    @Column(name = "duration_milliseconds")
    private Long durationMilliseconds;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feedback_snapshot", columnDefinition = "JSONB", nullable = false)
    private String feedbackSnapshot;
}
