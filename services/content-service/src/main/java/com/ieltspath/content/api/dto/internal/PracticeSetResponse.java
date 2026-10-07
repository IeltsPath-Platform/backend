package com.ieltspath.content.api.dto.internal;

import com.ieltspath.content.application.result.PracticeSetResult;

import java.util.UUID;

public record PracticeSetResponse(UUID packageId, UUID packageVersionId, String code, int questionCount,
                                  int matchedQuestionCount) {
    public static PracticeSetResponse from(PracticeSetResult result) {
        return new PracticeSetResponse(result.packageId(), result.packageVersionId(), result.code(),
                result.questionCount(), result.matchedQuestionCount());
    }
}
