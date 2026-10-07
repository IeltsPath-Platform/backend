package com.ieltspath.content.api.dto.internal;

import com.ieltspath.content.application.result.TopicTestPackageResult;

import java.util.UUID;

public record TopicTestPackageResponse(UUID packageId, UUID packageVersionId, String code) {
    public static TopicTestPackageResponse from(TopicTestPackageResult result) {
        return new TopicTestPackageResponse(result.packageId(), result.packageVersionId(), result.code());
    }
}
