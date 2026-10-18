package com.ieltspath.content.application.result;

import java.math.BigDecimal;
import java.util.UUID;

/** A practice set eligible for reviewing one knowledge point. */
public record PracticeSetResult(
        UUID packageId,
        UUID packageVersionId,
        String code,
        int questionCount,
        int matchedQuestionCount,
        BigDecimal band,
        String requiredFeatureKey
) {
    public PracticeSetResult(UUID packageId, UUID packageVersionId, String code, int questionCount,
                             int matchedQuestionCount) {
        this(packageId, packageVersionId, code, questionCount, matchedQuestionCount, null, null);
    }
}
