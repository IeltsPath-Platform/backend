package com.ieltspath.learning.domain.vo;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The decision on a course after a course test. {@code PASSED} is final. {@code REMEDIATION} waits for the reviews in
 * {@code reviewIds}; {@code reviewsPending} says whether any is still PENDING, and when none is the learner may retest.
 * {@code components} are the values the total was made of, keyed {@code practice}, {@code practiceTest}, {@code process},
 * {@code mid}, {@code final} (a missing key is a component the course does not have).
 */
public record CourseOutcome(UUID userId, UUID courseId, Status status, double total, Map<String, Double> components,
                            UUID finalAttemptId, List<String> topErrorCodes, List<UUID> reviewIds, Instant decidedAt,
                            boolean reviewsPending) {
    public enum Status { PASSED, REMEDIATION }

    public boolean retestReady() {
        return status == Status.REMEDIATION && !reviewsPending;
    }
}
