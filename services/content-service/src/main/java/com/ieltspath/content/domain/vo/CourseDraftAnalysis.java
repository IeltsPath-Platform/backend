package com.ieltspath.content.domain.vo;

import java.util.List;
import java.util.UUID;

/**
 * The result of checking a course draft against the catalogue. Errors stop an approval; warnings and gaps do not.
 */
public record CourseDraftAnalysis(List<Issue> errors, List<Issue> warnings, List<Gap> gaps, Coverage coverage) {

    public CourseDraftAnalysis {
        errors = List.copyOf(errors);
        warnings = List.copyOf(warnings);
        gaps = List.copyOf(gaps);
    }

    public boolean valid() {
        return errors.isEmpty();
    }

    /** Errors that mean "that name is already used", answered with a conflict instead of an invalid draft. */
    public List<Issue> conflicts() {
        return errors.stream().filter(Issue::isConflict).toList();
    }

    public record Issue(String code, String subject, String related) {
        public static final String CODE_TAKEN = "CODE_TAKEN";
        public static final String BAND_TAKEN = "BAND_TAKEN";
        public static final String TOPIC_SORT_ORDER_TAKEN = "TOPIC_SORT_ORDER_TAKEN";

        public boolean isConflict() {
            return CODE_TAKEN.equals(code) || BAND_TAKEN.equals(code) || TOPIC_SORT_ORDER_TAKEN.equals(code);
        }
    }

    /** {@code reason} is {@code NO_PRACTICE_SET}, {@code FEW_PRACTICE_SETS} or {@code NO_EXERCISE_QUESTION}. */
    public record Gap(UUID knowledgePointId, String reason) {
    }

    public record Coverage(int expected, int covered) {
    }
}
