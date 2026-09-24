package com.group01.assessment.infrastructure.persistence.entity;

import com.group01.assessment.domain.vo.GradingSource;
import com.group01.assessment.domain.vo.Skill;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "skill_scores", uniqueConstraints = @UniqueConstraint(columnNames = {"result_id", "skill"}))
@Getter
@Setter
@NoArgsConstructor
public class SkillScoreJpaEntity {
    @Id
    private UUID id;
    @Column(name = "result_id", nullable = false)
    private UUID resultId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Skill skill;
    @Column(name = "raw_score", precision = 8, scale = 2)
    private BigDecimal rawScore;
    @Column(precision = 3, scale = 1)
    private BigDecimal band;
    @Enumerated(EnumType.STRING)
    @Column(name = "grading_source", nullable = false)
    private GradingSource gradingSource;
    @Column(name = "feedback_revision_id")
    private UUID feedbackRevisionId;
}
