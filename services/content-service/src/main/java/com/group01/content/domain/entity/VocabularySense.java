package com.group01.content.domain.entity;

import com.group01.content.domain.vo.ContentStatus;
import com.group01.content.domain.vo.PartOfSpeech;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class VocabularySense {
    private final UUID id;
    private final UUID vocabularyItemId;
    private PartOfSpeech partOfSpeech;
    private String englishDefinition;
    private String vietnameseMeaning;
    private String exampleSentence;
    private String imageUrl;
    private int sortOrder;
    private ContentStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public VocabularySense(UUID id, UUID vocabularyItemId, PartOfSpeech partOfSpeech,
                           String englishDefinition, String vietnameseMeaning, String exampleSentence,
                           String imageUrl, int sortOrder, ContentStatus status,
                           Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.vocabularyItemId = Objects.requireNonNull(vocabularyItemId, "vocabularyItemId must not be null");
        this.partOfSpeech = Objects.requireNonNull(partOfSpeech, "partOfSpeech must not be null");
        this.englishDefinition = englishDefinition;
        this.vietnameseMeaning = Objects.requireNonNull(vietnameseMeaning, "vietnameseMeaning must not be null");
        this.exampleSentence = Objects.requireNonNull(exampleSentence, "exampleSentence must not be null");
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        this.status = status != null ? status : ContentStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static VocabularySense create(UUID vocabularyItemId, PartOfSpeech partOfSpeech,
                                         String englishDefinition, String vietnameseMeaning,
                                         String exampleSentence, String imageUrl, int sortOrder) {
        Instant now = Instant.now();
        return new VocabularySense(UUID.randomUUID(), vocabularyItemId, partOfSpeech, englishDefinition,
                vietnameseMeaning, exampleSentence, imageUrl, sortOrder, ContentStatus.ACTIVE, now, now);
    }

    public void update(PartOfSpeech partOfSpeech, String englishDefinition, String vietnameseMeaning,
                       String exampleSentence, String imageUrl, int sortOrder, ContentStatus status) {
        this.partOfSpeech = Objects.requireNonNull(partOfSpeech, "partOfSpeech must not be null");
        this.englishDefinition = englishDefinition;
        this.vietnameseMeaning = Objects.requireNonNull(vietnameseMeaning, "vietnameseMeaning must not be null");
        this.exampleSentence = Objects.requireNonNull(exampleSentence, "exampleSentence must not be null");
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        if (status != null) {
            this.status = status;
        }
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getVocabularyItemId() { return vocabularyItemId; }
    public PartOfSpeech getPartOfSpeech() { return partOfSpeech; }
    public String getEnglishDefinition() { return englishDefinition; }
    public String getVietnameseMeaning() { return vietnameseMeaning; }
    public String getExampleSentence() { return exampleSentence; }
    public String getImageUrl() { return imageUrl; }
    public int getSortOrder() { return sortOrder; }
    public ContentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

