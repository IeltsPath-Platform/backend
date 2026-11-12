package com.ieltspath.learning.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Writing progress for the learner, one block per task asked for; contract {@code writing-progress-v1}. */
public record WritingProgressResult(List<TaskProgress> tasks) {

    /** {@code insufficient} is true below three essays: the trend and the fixed list are then not meaningful. */
    public record TaskProgress(String task, List<Point> points, List<RecurringError> recurringErrors,
                               List<FixedError> fixedErrors, boolean insufficient) {}

    /** One graded first essay, oldest first; {@code criteria} maps TA/TR, CC, LR, GRA to bands. */
    public record Point(UUID submissionId, String source, Instant gradedAt, BigDecimal overallBand,
                        Map<String, BigDecimal> criteria, String promptVersion) {}

    /** An error code seen in the window; {@code essaysWithError} counts essays, not corrections. */
    public record RecurringError(String errorCode, String errorName, String fixHint, UUID practiceKnowledgePointId,
                                 String practiceKnowledgePointCode, int essaysWithError, int essaysTotal,
                                 Instant lastSeenAt) {}

    /** An error code absent from the last {@code cleanEssaysSince} essays of its task. A signal, not a mastery verdict. */
    public record FixedError(String errorCode, String errorName, Instant lastSeenAt, int cleanEssaysSince) {}
}
