package com.group01.content.domain.vo;

import com.group01.content.domain.exception.InvalidLessonBlockException;

import java.math.BigDecimal;
import java.util.List;

/**
 * What an {@code EXERCISE} block asks of the learner: auto-graded questions, or one essay graded elsewhere.
 * Content has no authoring API, so this check on read is what keeps a mixed or malformed block out of lessons.
 */
public enum LessonBlockKind {
    EXERCISE,
    ESSAY;

    public static final String ESSAY_SPEC_TYPE = "ESSAY";
    private static final BigDecimal MIN_PASS_BAND = new BigDecimal("4.0");
    private static final BigDecimal MAX_PASS_BAND = new BigDecimal("9.0");
    private static final BigDecimal HALF_BAND = new BigDecimal("0.5");

    /** The answer-spec facts classification needs; {@code passBand} is the raw JSON value, null when absent. */
    public record QuestionSpec(String type, String passBand) {}

    public static LessonBlockKind classify(List<QuestionSpec> questions) {
        long essays = questions.stream().filter(q -> ESSAY_SPEC_TYPE.equals(q.type())).count();
        if (essays == 0) {
            return EXERCISE;
        }
        if (essays > 1 || questions.size() > 1) {
            throw new InvalidLessonBlockException("An essay block must hold exactly one question");
        }
        requireValidPassBand(questions.get(0).passBand());
        return ESSAY;
    }

    private static void requireValidPassBand(String raw) {
        BigDecimal band;
        try {
            band = raw == null ? null : new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            band = null;
        }
        if (band == null || band.compareTo(MIN_PASS_BAND) < 0 || band.compareTo(MAX_PASS_BAND) > 0
                || band.remainder(HALF_BAND).signum() != 0) {
            throw new InvalidLessonBlockException("Essay passBand must be 4.0-9.0 in steps of 0.5");
        }
    }
}
