package com.group01.content.infrastructure.persistence.entity;

import com.group01.content.domain.vo.ContentStatus;
import com.group01.content.domain.vo.PartOfSpeech;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vocabulary_senses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VocabularySenseJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "vocabulary_item_id", nullable = false)
    private UUID vocabularyItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "part_of_speech", nullable = false, length = 50)
    private PartOfSpeech partOfSpeech;

    @Column(name = "english_definition", columnDefinition = "TEXT")
    private String englishDefinition;

    @Column(name = "vietnamese_meaning", nullable = false, columnDefinition = "TEXT")
    private String vietnameseMeaning;

    @Column(name = "example_sentence", nullable = false, columnDefinition = "TEXT")
    private String exampleSentence;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ContentStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

