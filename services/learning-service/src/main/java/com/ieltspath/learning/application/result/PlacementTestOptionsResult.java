package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.vo.PlacementPool;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlacementTestOptionsResult(List<PoolOption> pools, int limitPer24h, int retakesLeft,
                                         Instant nextRetakeAt, boolean hasPlacement, Double currentBand,
                                         PlacementAssignmentResult openAssignment) {
    public record PoolOption(PlacementPool pool, int examCount) {}
}
