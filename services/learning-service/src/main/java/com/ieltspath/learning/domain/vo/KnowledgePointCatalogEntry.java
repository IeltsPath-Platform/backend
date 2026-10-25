package com.ieltspath.learning.domain.vo;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** {@code band} is the band of the course the point is taught in, when it has one. */
public record KnowledgePointCatalogEntry(UUID knowledgePointId, UUID topicId, boolean hasPracticeSet,
                                         LearningSkill skill, BigDecimal band, UUID parentKnowledgePointId,
                                         String errorCode) {
    public KnowledgePointCatalogEntry(UUID knowledgePointId, UUID topicId, boolean hasPracticeSet,
                                      LearningSkill skill, BigDecimal band) {
        this(knowledgePointId, topicId, hasPracticeSet, skill, band, null, null);
    }
    public KnowledgePointCatalogEntry(UUID knowledgePointId, UUID topicId, boolean hasPracticeSet,
                                      LearningSkill skill) {
        this(knowledgePointId, topicId, hasPracticeSet, skill, null, null, null);
    }
    public KnowledgePointCatalogEntry(UUID knowledgePointId, UUID topicId, boolean hasPracticeSet) {
        this(knowledgePointId, topicId, hasPracticeSet, null, null, null, null);
    }
    public KnowledgePointCatalogEntry {
        Objects.requireNonNull(knowledgePointId, "knowledgePointId");
        Objects.requireNonNull(topicId, "topicId");
    }
}
