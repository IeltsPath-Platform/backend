package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.application.result.PlacementTestOptionsResult;
import com.ieltspath.learning.domain.vo.PlacementPool;

import java.time.Instant;
import java.util.List;

public record PlacementTestOptionsResponse(List<PoolOption> pools, int limitPer24h, int retakesLeft,
                                           Instant nextRetakeAt, boolean hasPlacement, Double currentBand,
                                           PlacementAssignmentResponse openAssignment) {
    public record PoolOption(PlacementPool pool, int examCount) {}
    public static PlacementTestOptionsResponse from(PlacementTestOptionsResult r) {
        return new PlacementTestOptionsResponse(r.pools().stream()
                .map(p -> new PoolOption(p.pool(), p.examCount())).toList(), r.limitPer24h(), r.retakesLeft(),
                r.nextRetakeAt(), r.hasPlacement(), r.currentBand(), PlacementAssignmentResponse.from(r.openAssignment()));
    }
}
