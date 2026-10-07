package com.ieltspath.content.application.result;

import java.util.UUID;

/** A practice set eligible for reviewing one knowledge point. */
public record PracticeSetResult(
        UUID packageId,
        UUID packageVersionId,
        String code,
        int questionCount,
        int matchedQuestionCount
) {}
