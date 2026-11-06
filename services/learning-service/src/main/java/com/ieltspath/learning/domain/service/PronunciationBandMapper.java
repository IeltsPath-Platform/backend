package com.ieltspath.learning.domain.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Turns the speech service's pronunciation score (0 to 100, graded against a native speaker) into an IELTS band for
 * Pronunciation. The language model never gives this band. The table has a version that is stored with every grade, so a
 * grade can be traced to the table that made it.
 *
 * <p>{@link #INTERIM} is a placeholder with no calibration behind it: it is not fitted to examiner scores and the service
 * scores a native speaker's reading near 93, so the top of the table is guesswork. It must be replaced by a table
 * fitted to examiner marks before AI grading is opened to learners.
 */
public record PronunciationBandMapper(String version, List<Step> steps) {

    /** A score from {@code minScore} up (until the next step) maps to {@code band}. Steps are sorted from the lowest. */
    public record Step(double minScore, BigDecimal band) { }

    public static final PronunciationBandMapper INTERIM = new PronunciationBandMapper("pr-interim-1", List.of(
            new Step(0, new BigDecimal("3.0")),
            new Step(40, new BigDecimal("4.0")),
            new Step(55, new BigDecimal("5.0")),
            new Step(65, new BigDecimal("5.5")),
            new Step(72, new BigDecimal("6.0")),
            new Step(78, new BigDecimal("6.5")),
            new Step(84, new BigDecimal("7.0")),
            new Step(89, new BigDecimal("7.5")),
            new Step(93, new BigDecimal("8.0"))));

    public PronunciationBandMapper {
        if (steps == null || steps.isEmpty()) throw new IllegalArgumentException("the table needs at least one step");
        for (int index = 1; index < steps.size(); index++) {
            if (steps.get(index).minScore() <= steps.get(index - 1).minScore()) {
                throw new IllegalArgumentException("steps must rise in score");
            }
        }
        steps = List.copyOf(steps);
    }

    public BigDecimal band(double score) {
        BigDecimal band = steps.get(0).band();
        for (Step step : steps) {
            if (score >= step.minScore()) band = step.band();
        }
        return band;
    }
}
