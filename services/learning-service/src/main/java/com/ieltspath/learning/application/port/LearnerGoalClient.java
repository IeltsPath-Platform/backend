package com.ieltspath.learning.application.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** The learner's goal, kept by Learning. A goal is optional context: its absence never blocks a report. */
public interface LearnerGoalClient {
    /** The goal, or empty when the learner has none or the goal cannot be read. Never throws. */
    Optional<Goal> goal(UUID userId);

    /** Both values are null when the learner has not set a goal. */
    record Goal(BigDecimal targetBand, LocalDate examDate) {}
}
