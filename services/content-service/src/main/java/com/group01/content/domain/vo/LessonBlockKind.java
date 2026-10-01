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
    private static final String TASK_1 = "TASK_1";
    private static final String TASK_2 = "TASK_2";
    private static final int MAX_CHART_FACTS = 2000;
    private static final BigDecimal MIN_PASS_BAND = new BigDecimal("4.0");
    private static final BigDecimal MAX_PASS_BAND = new BigDecimal("9.0");
    private static final BigDecimal HALF_BAND = new BigDecimal("0.5");

    /**
     * The answer-spec facts classification needs, read as raw JSON text (null when absent), plus the types of the
     * assets attached to the question version.
     */
    public record QuestionSpec(String type, String task, String passBand, String chartFacts,
                               List<AssetType> assetTypes) {
        public QuestionSpec {
            assetTypes = assetTypes == null ? List.of() : List.copyOf(assetTypes);
        }
    }

    public static LessonBlockKind classify(List<QuestionSpec> questions) {
        long essays = questions.stream().filter(q -> ESSAY_SPEC_TYPE.equals(q.type())).count();
        if (essays == 0) {
            return EXERCISE;
        }
        if (essays > 1 || questions.size() > 1) {
            throw new InvalidLessonBlockException("An essay block must hold exactly one question");
        }
        QuestionSpec essay = questions.get(0);
        requireValidPassBand(essay.passBand());
        if (TASK_1.equals(essay.task())) {
            requireTask1Chart(essay);
        } else if (!TASK_2.equals(essay.task())) {
            throw new InvalidLessonBlockException("Essay task must be TASK_1 or TASK_2");
        }
        return ESSAY;
    }

    /** Task 1 is graded against chartFacts and shown with the chart image, so both must exist. */
    private static void requireTask1Chart(QuestionSpec essay) {
        String facts = essay.chartFacts();
        if (facts == null || facts.isBlank() || facts.length() > MAX_CHART_FACTS) {
            throw new InvalidLessonBlockException("Task 1 essay needs chartFacts of 1-2000 characters");
        }
        if (!essay.assetTypes().contains(AssetType.IMAGE)) {
            throw new InvalidLessonBlockException("Task 1 essay needs at least one chart image");
        }
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
