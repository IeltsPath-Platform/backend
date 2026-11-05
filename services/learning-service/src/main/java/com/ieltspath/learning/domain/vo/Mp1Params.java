package com.ieltspath.learning.domain.vo;

import java.util.Objects;

/**
 * Parameters of the MP-1.0 mastery formula. {@code window} newest valid answers count, at most
 * {@code maxPracticeInWindow} of them from practice; each is weighed by its source and halves every
 * {@code halfLifeDays}. A point is judged only with {@code minEvidence} answers over {@code minDistinctDays} days and
 * a decayed weight of {@code minConfidence}. {@code hysteresis} is the margin a score must pass a tier threshold by
 * before the tier changes. The same question inside {@code sameItemDays} counts once.
 */
public record Mp1Params(int window, int maxPracticeInWindow, double halfLifeDays, int minEvidence, int minDistinctDays,
                        double minConfidence, double hysteresis, int sameItemDays, SourceWeights weights) {
    /** Weight of one answer by where it came from. */
    public record SourceWeights(double exam, double review, double probe, double practice, double essay,
                                double errorTrap) {
        public SourceWeights(double exam, double review, double probe, double practice, double essay) {
            this(exam, review, probe, practice, essay, 0.3);
        }
        public double of(EvidenceSource source) {
            return switch (source) {
                case ASSESSMENT -> exam;
                case REVIEW_SET -> review;
                case DIAGNOSTIC_PROBE -> probe;
                case LESSON_EXERCISE, PRACTICE_SET, FREE_PRACTICE -> practice;
                case LESSON_WRITING -> essay;
                case ERROR_TRAP -> errorTrap;
            };
        }
    }

    public Mp1Params {
        Objects.requireNonNull(weights, "weights");
        if (window < 1 || maxPracticeInWindow < 0 || halfLifeDays <= 0 || minEvidence < 1 || minDistinctDays < 1
                || minConfidence < 0 || hysteresis < 0 || hysteresis >= 0.1 || sameItemDays < 0) {
            throw new IllegalArgumentException("Invalid MP-1.0 parameters");
        }
    }

    public static Mp1Params defaults() {
        return new Mp1Params(8, 3, 30, 3, 2, 1.0, 0.03, 7,
                new SourceWeights(1.0, 1.0, 1.0, 0.6, 0.5, 0.3));
    }

    /** True for evidence that counts against {@code maxPracticeInWindow}. */
    public static boolean isPractice(EvidenceSource source) {
        return source == EvidenceSource.LESSON_EXERCISE || source == EvidenceSource.PRACTICE_SET
                || source == EvidenceSource.FREE_PRACTICE;
    }
}
