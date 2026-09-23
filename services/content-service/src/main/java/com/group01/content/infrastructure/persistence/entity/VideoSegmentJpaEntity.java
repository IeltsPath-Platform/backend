package com.group01.content.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "video_segments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoSegmentJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "start_ms", nullable = false)
    private int startMs;

    @Column(name = "end_ms", nullable = false)
    private int endMs;

    @Column(name = "transcript", nullable = false, columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "translation_vi", columnDefinition = "TEXT")
    private String translationVi;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "segmentId", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startChar ASC")
    @Builder.Default
    private List<VideoSegmentLexicalEntryJpaEntity> lexicalEntries = new ArrayList<>();
}

