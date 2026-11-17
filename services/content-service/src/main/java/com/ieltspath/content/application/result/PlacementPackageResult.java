package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.PlacementPool;
import java.util.UUID;

public record PlacementPackageResult(UUID packageId, UUID packageVersionId, String code, PlacementPool pool) {}
