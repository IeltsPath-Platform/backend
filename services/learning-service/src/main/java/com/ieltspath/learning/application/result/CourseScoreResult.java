package com.ieltspath.learning.application.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A learner's score in a course. Values are fractions of 1; a component is null when the course does not have it.
 * {@code status}: {@code IN_PROGRESS} before any course test, {@code PASSED} (the stored decision, not recalculated),
 * {@code REMEDIATION} while the reviews from the last course test are open, {@code RETEST_READY} once they are done.
 * Until the course is passed, {@code total} follows the current configuration. {@code topErrorCodes} are the most frequent
 * errors of the last course test. {@code weights} are the ones applied now, after sharing out a missing component.
 */
public record CourseScoreResult(UUID courseId, String status, Components components, Weights weights, double total,
                                double passThreshold, double midRemediationBelow, List<String> topErrorCodes,
                                List<Flag> remediation, Instant decidedAt) {
    public record Components(Double practice, Double practiceTest, Double process, Double mid, Double finalTest) {}

    public record Weights(double process, double mid, double finalTest, double practiceShare, double practiceTestShare) {}

    public record Flag(String kind, UUID topicId, boolean open, Instant createdAt) {}
}
