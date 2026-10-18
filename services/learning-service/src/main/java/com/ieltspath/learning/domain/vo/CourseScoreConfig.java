package com.ieltspath.learning.domain.vo;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * How the course score is put together, all as fractions of 1. {@code courseId} is null for the default every course
 * without its own configuration follows. Process = {@code practiceShare} Practice + {@code practiceTestShare} milestone
 * practice tests; the total is {@code processWeight} Process + {@code midWeight} mid-course + {@code finalWeight} final.
 * A learner passes the course at {@code passThreshold}; a first topic test below {@code midRemediationBelow} asks for
 * supplementary work.
 */
public record CourseScoreConfig(UUID courseId, BigDecimal processWeight, BigDecimal midWeight, BigDecimal finalWeight,
                                BigDecimal practiceShare, BigDecimal practiceTestShare, BigDecimal passThreshold,
                                BigDecimal midRemediationBelow) {
    public CourseScoreConfig {
        requireFraction(processWeight, "processWeight");
        requireFraction(midWeight, "midWeight");
        requireFraction(finalWeight, "finalWeight");
        requireFraction(practiceShare, "practiceShare");
        requireFraction(practiceTestShare, "practiceTestShare");
        requireFraction(midRemediationBelow, "midRemediationBelow");
        if (processWeight.add(midWeight).add(finalWeight).compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("processWeight, midWeight and finalWeight must add up to 1");
        }
        if (practiceShare.add(practiceTestShare).compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("practiceShare and practiceTestShare must add up to 1");
        }
        if (passThreshold == null || passThreshold.signum() <= 0 || passThreshold.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("passThreshold must be in (0, 1]");
        }
    }

    private static void requireFraction(BigDecimal value, String name) {
        if (value == null || value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(name + " must be between 0 and 1");
        }
    }

    public CourseScoreConfig forCourse(UUID course) {
        return new CourseScoreConfig(course, processWeight, midWeight, finalWeight, practiceShare, practiceTestShare,
                passThreshold, midRemediationBelow);
    }
}
