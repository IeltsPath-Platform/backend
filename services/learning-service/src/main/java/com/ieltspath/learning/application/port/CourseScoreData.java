package com.ieltspath.learning.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The learner's results that feed the course score; percents are fractions of 1. */
public interface CourseScoreData {
    /** Mean of the first counted Practice submissions of the course's lessons; empty before the first one. */
    Optional<Double> practiceAverage(UUID userId, UUID courseId);

    /** The learner's finished milestone practice tests of the course. */
    List<Double> practiceTestPercents(UUID userId, UUID courseId);

    /** The newest finished course test. */
    Optional<FinalTest> latestFinal(UUID userId, UUID courseId);

    /** The most frequent error codes of the wrong answers in one assessment attempt, most frequent first. */
    List<ErrorCount> topErrors(UUID userId, UUID attemptId, int limit);

    record FinalTest(UUID attemptId, double percent) {}

    record ErrorCount(String code, int count) {}
}
