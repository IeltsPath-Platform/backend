package com.group01.content.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "video_segment_lexical_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoSegmentLexicalEntryJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "segment_id", nullable = false)
    private UUID segmentId;

    @Column(name = "vocabulary_sense_id")
    private UUID vocabularySenseId;

    @Column(name = "surface_text", nullable = false)
    private String surfaceText;

    @Column(name = "start_char", nullable = false)
    private int startChar;

    @Column(name = "end_char", nullable = false)
    private int endChar;

    @Column(name = "sort_order")
    private int sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

