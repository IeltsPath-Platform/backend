package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.vo.PlacementPool;

import java.time.Instant;
import java.util.UUID;

public record PlacementAssignmentResult(UUID assignmentId, PlacementPool pool, UUID packageId,
                                         UUID packageVersionId, int retakesLeft, Instant nextRetakeAt) {}
