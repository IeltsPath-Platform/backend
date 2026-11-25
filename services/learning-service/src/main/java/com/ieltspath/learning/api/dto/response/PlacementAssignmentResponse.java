package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.application.result.PlacementAssignmentResult;
import com.ieltspath.learning.domain.vo.PlacementPool;

import java.time.Instant;
import java.util.UUID;

public record PlacementAssignmentResponse(UUID assignmentId, PlacementPool pool, UUID packageId,
                                          UUID packageVersionId, int retakesLeft, Instant nextRetakeAt) {
    public static PlacementAssignmentResponse from(PlacementAssignmentResult r) {
        return r == null ? null : new PlacementAssignmentResponse(r.assignmentId(), r.pool(), r.packageId(),
                r.packageVersionId(), r.retakesLeft(), r.nextRetakeAt());
    }
}
