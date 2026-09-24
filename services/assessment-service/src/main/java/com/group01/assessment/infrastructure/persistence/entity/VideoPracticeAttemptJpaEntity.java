package com.group01.assessment.infrastructure.persistence.entity;

import com.group01.assessment.domain.vo.PracticeStatus;
import com.group01.assessment.domain.vo.PracticeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "video_practice_attempts")
@Getter
@Setter
@NoArgsConstructor
public class VideoPracticeAttemptJpaEntity {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "video_id", nullable = false)
    private UUID videoId;
    @Column(name = "segment_id", nullable = false)
    private UUID segmentId;
    @Enumerated(EnumType.STRING)
    @Column(name = "practice_type", nullable = false)
    private PracticeType practiceType;
    @Column(name = "reference_text_snapshot", nullable = false, columnDefinition = "TEXT")
    private String referenceTextSnapshot;
    @Column(name = "response_text", columnDefinition = "TEXT")
    private String responseText;
    @Column(name = "audio_reference", length = 500)
    private String audioReference;
    @Column(precision = 5, scale = 2)
    private BigDecimal score;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_payload", columnDefinition = "JSONB")
    private String resultPayload;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PracticeStatus status;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
