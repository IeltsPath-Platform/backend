package com.group01.learning.domain.vo;

import java.util.Objects;
import java.util.UUID;

public record KnowledgePointCatalogEntry(UUID knowledgePointId, UUID topicId, boolean hasPracticeSet) {
    public KnowledgePointCatalogEntry {
        Objects.requireNonNull(knowledgePointId, "knowledgePointId");
        Objects.requireNonNull(topicId, "topicId");
    }
}
