package com.group01.assessment.domain.entity;

import com.group01.assessment.domain.vo.QualitativeJudgment;

import java.util.Objects;
import java.util.UUID;

public record ItemResultKnowledgeJudgment(UUID itemResultId, UUID knowledgePointId, QualitativeJudgment judgment) {
    public ItemResultKnowledgeJudgment {
        Objects.requireNonNull(itemResultId, "itemResultId must not be null");
        Objects.requireNonNull(knowledgePointId, "knowledgePointId must not be null");
        Objects.requireNonNull(judgment, "judgment must not be null");
    }
}
