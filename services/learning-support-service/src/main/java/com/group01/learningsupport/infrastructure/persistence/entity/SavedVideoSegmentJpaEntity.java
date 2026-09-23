package com.group01.learningsupport.infrastructure.persistence.entity;

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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saved_video_segments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedVideoSegmentJpaEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(name = "segment_id", nullable = false)
    private UUID segmentId;

    @Column(name = "transcript_snapshot", nullable = false, columnDefinition = "text")
    private String transcriptSnapshot;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void touch() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
