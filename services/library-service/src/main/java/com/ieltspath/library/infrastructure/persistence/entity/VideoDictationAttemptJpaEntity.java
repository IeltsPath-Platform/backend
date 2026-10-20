package com.ieltspath.library.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "video_dictation_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoDictationAttemptJpaEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(name = "segment_id", nullable = false)
    private UUID segmentId;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(name = "typed_text", nullable = false, columnDefinition = "TEXT")
    private String typedText;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "accuracy_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal accuracyPercent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
