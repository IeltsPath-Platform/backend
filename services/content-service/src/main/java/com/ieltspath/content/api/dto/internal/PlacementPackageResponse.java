package com.ieltspath.content.api.dto.internal;

import com.ieltspath.content.application.result.PlacementPackageResult;
import com.ieltspath.content.domain.vo.PlacementPool;
import java.util.UUID;

public record PlacementPackageResponse(UUID packageId, UUID packageVersionId, String code, PlacementPool pool) {
    public static PlacementPackageResponse from(PlacementPackageResult result) {
        return new PlacementPackageResponse(result.packageId(), result.packageVersionId(), result.code(), result.pool());
    }
}
