package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.application.result.PlacementTestResult;

import java.util.UUID;

public record PlacementTestResponse(UUID packageId, UUID packageVersionId) {
    public static PlacementTestResponse from(PlacementTestResult result) {
        return new PlacementTestResponse(result.packageId(), result.packageVersionId());
    }
}
