package com.group01.assessment.infrastructure.persistence.entity;

import com.group01.assessment.domain.vo.Skill;
import com.group01.assessment.domain.vo.SubmissionStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "learner_submissions", uniqueConstraints = @UniqueConstraint(name = "uk_submission_key", columnNames = {"user_id", "submission_key"}))
@Getter @Setter @NoArgsConstructor
public class LearnerSubmissionJpaEntity {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "attempt_item_id") private UUID attemptItemId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "prompt_snapshot", columnDefinition = "JSONB", nullable = false) private String promptSnapshot;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Skill skill;
    @Column(name = "text_payload") private String textPayload;
    @Column(name = "audio_reference") private String audioReference;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SubmissionStatus status;
    @Column(name = "submission_key", nullable = false) private String submissionKey;
    @Column(name = "submitted_at", nullable = false) private Instant submittedAt;
}
