package com.group01.content.domain.entity;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class QuestionKnowledgePoint {
    private final UUID questionVersionId;
    private final UUID knowledgePointId;
    private final BigDecimal weight;

    public QuestionKnowledgePoint(UUID questionVersionId, UUID knowledgePointId, BigDecimal weight) {
        this.questionVersionId = Objects.requireNonNull(questionVersionId, "questionVersionId must not be null");
        this.knowledgePointId = Objects.requireNonNull(knowledgePointId, "knowledgePointId must not be null");
        this.weight = weight != null ? weight : BigDecimal.ONE;
    }

    public UUID getQuestionVersionId() { return questionVersionId; }
    public UUID getKnowledgePointId() { return knowledgePointId; }
    public BigDecimal getWeight() { return weight; }
}

