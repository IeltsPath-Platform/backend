package com.group01.assessment.infrastructure.persistence.entity;

import com.group01.assessment.domain.vo.AttemptChannel;
import com.group01.assessment.domain.vo.AttemptMode;
import com.group01.assessment.domain.vo.AttemptStatus;
import com.group01.assessment.domain.vo.AttemptType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessment_attempts")
@Getter @Setter @NoArgsConstructor
public class AssessmentAttemptJpaEntity {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "package_version_id", nullable = false)
    private UUID packageVersionId;
    @Enumerated(EnumType.STRING) @Column(name = "attempt_type", nullable = false)
    private AttemptType attemptType;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AttemptMode mode;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AttemptChannel channel;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AttemptStatus status;
    @Column(name = "started_at")
    private Instant startedAt;
    private Instant submittedAt;
    private Instant expiresAt;
    @Version @Column(name = "row_version", nullable = false)
    private long rowVersion;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "learning_goal_id")
    private UUID learningGoalId;
}