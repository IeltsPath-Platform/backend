package com.ieltspath.learning.application.result;

import java.util.UUID;

/** The published placement test a learner sits before choosing a course. */
public record PlacementTestResult(UUID packageId, UUID packageVersionId) {
}
