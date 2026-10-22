package com.ieltspath.learning.domain.vo;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** A mapped rubric criterion whose band is below the essay's pass band. */
public record FailedWritingCriterion(UUID knowledgePointId, String code, BigDecimal band) {
    public FailedWritingCriterion {
        Objects.requireNonNull(knowledgePointId, "knowledgePointId");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(band, "band");
    }
}
