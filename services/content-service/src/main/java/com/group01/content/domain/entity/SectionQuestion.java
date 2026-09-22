package com.group01.content.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class SectionQuestion {
    private final UUID id;
    private final UUID sectionId;
    private final UUID questionVersionId;
    private int sortOrder;
    private BigDecimal maxScore;
    private final Instant createdAt;

    public SectionQuestion(UUID id, UUID sectionId, UUID questionVersionId,
                           int sortOrder, BigDecimal maxScore, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.sectionId = Objects.requireNonNull(sectionId, "sectionId must not be null");
        this.questionVersionId = Objects.requireNonNull(questionVersionId, "questionVersionId must not be null");
        this.sortOrder = sortOrder;
        this.maxScore = maxScore != null ? maxScore : BigDecimal.ONE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static SectionQuestion create(UUID sectionId, UUID questionVersionId, int sortOrder, BigDecimal maxScore) {
        return new SectionQuestion(UUID.randomUUID(), sectionId, questionVersionId, sortOrder, maxScore, Instant.now());
    }

    public UUID getId() { return id; }
    public UUID getSectionId() { return sectionId; }
    public UUID getQuestionVersionId() { return questionVersionId; }
    public int getSortOrder() { return sortOrder; }
    public BigDecimal getMaxScore() { return maxScore; }
    public Instant getCreatedAt() { return createdAt; }
}

