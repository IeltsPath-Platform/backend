package com.group01.learningsupport.infrastructure.persistence.entity;

import com.group01.learningsupport.domain.vo.VideoProgressStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "video_learning_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoLearningProgressJpaEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(name = "last_position_ms", nullable = false)
    private int lastPositionMs;

    @Column(name = "watched_duration_seconds", nullable = false)
    private int watchedDurationSeconds;

    @Column(name = "progress_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal progressPercent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VideoProgressStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "last_watched_at")
    private Instant lastWatchedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
