package com.ieltspath.learning.domain.vo;

import java.time.Instant;
import java.util.UUID;

/** Records of the staff side: learner flags, work items and the statistics work items are made from. */
public final class StaffRecords {
    private StaffRecords() {}

    public enum FlagKind { TOO_FAST_HIGH_SCORE, BURST_SUBMISSIONS, DUPLICATE_ESSAY, INACTIVE }

    public enum FlagStatus { OPEN, CONFIRMED, DISMISSED }

    /** {@code evidence} is a JSON object explaining why the flag was raised. */
    public record LearnerFlag(UUID id, UUID userId, FlagKind kind, String evidence, FlagStatus status, UUID reviewedBy,
                              String note, Instant createdAt, Instant reviewedAt) {}

    public enum Queue { EXAMINER, CONTENT_AUTHOR }

    public enum WorkKind {
        MID_COURSE_REMEDIATION, POST_COURSE_REMEDIATION, LEARNER_FLAG, CONTENT_GAP, QUESTION_TOO_EASY,
        QUESTION_TOO_HARD, EDGE_SUSPECT
    }

    public enum WorkStatus { OPEN, IN_PROGRESS, DONE }

    /** {@code subjectRef} is what the work is about (a flag, a knowledge point, a question or an edge); {@code detail} is JSON. */
    public record WorkItem(UUID id, Queue queue, WorkKind kind, UUID subjectUserId, UUID subjectRef, String detail,
                           WorkStatus status, UUID assigneeId, String action, String result, String note,
                           boolean noteVisibleToLearner, Instant createdAt, Instant updatedAt) {}

    /** First submissions of one question: how many, and how many were right. */
    public record QuestionStat(UUID questionVersionId, int attempts, int correct, Long medianDurationMs,
                               java.util.List<String> topErrorCodes, String difficulty) {
        public QuestionStat(UUID questionVersionId, int attempts, int correct, Long medianDurationMs,
                            java.util.List<String> topErrorCodes) {
            this(questionVersionId, attempts, correct, medianDurationMs, topErrorCodes, null);
        }

        public double correctRate() { return attempts == 0 ? 0 : (double) correct / attempts; }

        /** The same statistics with the difficulty tag Content gives the question; null when it has none. */
        public QuestionStat withDifficulty(String tag) {
            return new QuestionStat(questionVersionId, attempts, correct, medianDurationMs, topErrorCodes, tag);
        }
    }

    /** Probes of prerequisite {@code from} made while diagnosing {@code to}, from concluded diagnoses. */
    public record EdgeStat(UUID from, UUID to, int probes, int passed, int concludedAsRoot) {
        public double passRate() { return probes == 0 ? 0 : (double) passed / probes; }
        public double rootRate() { return probes == 0 ? 0 : (double) concludedAsRoot / probes; }
    }

    /** How often a knowledge point was the gap of a diagnosis, and how it fares. */
    public record KnowledgePointStat(UUID knowledgePointId, int learners, double averageMastery, int timesAsRoot,
                                     int probes, int probesFailed) {}
}
