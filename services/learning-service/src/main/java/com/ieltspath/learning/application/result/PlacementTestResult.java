package com.ieltspath.learning.application.result;

import java.util.UUID;

/** The published placement test handed to a client that predates the pools. */
public record PlacementTestResult(UUID packageId, UUID packageVersionId) {
}
