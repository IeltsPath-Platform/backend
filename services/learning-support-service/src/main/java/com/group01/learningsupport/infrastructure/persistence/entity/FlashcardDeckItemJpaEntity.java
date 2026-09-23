package com.group01.learningsupport.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flashcard_deck_items")
@IdClass(FlashcardDeckItemKey.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardDeckItemJpaEntity {
    @Id
    @Column(name = "deck_id", nullable = false)
    private UUID deckId;
    @Id
    @Column(name = "flashcard_id", nullable = false)
    private UUID flashcardId;
    @Column(name = "sort_order")
    private Integer sortOrder;
    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    @PrePersist
    void onCreate() {
        if (addedAt == null) {
            addedAt = Instant.now();
        }
    }
}
