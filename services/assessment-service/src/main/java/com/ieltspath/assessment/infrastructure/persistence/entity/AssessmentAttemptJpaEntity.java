package com.ieltspath.assessment.infrastructure.persistence.entity;

import com.ieltspath.assessment.domain.vo.AttemptChannel;
import com.ieltspath.assessment.domain.vo.AttemptMode;
import com.ieltspath.assessment.domain.vo.AttemptStatus;
import com.ieltspath.assessment.domain.vo.AttemptType;
import com.ieltspath.assessment.domain.vo.AttemptScope;
import com.ieltspath.assessment.domain.vo.Skill;
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
    @Column(name = "package_id")
    private UUID packageId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private AttemptScope scope = AttemptScope.FULL;
    @Enumerated(EnumType.STRING) @Column(length = 20)
    private Skill skill;
}
