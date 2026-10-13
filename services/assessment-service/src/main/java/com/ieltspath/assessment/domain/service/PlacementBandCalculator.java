package com.ieltspath.assessment.domain.service;

import com.ieltspath.assessment.domain.vo.Skill;

import java.util.Collection;

/**
 * Turns placement results into IELTS-style bands: Listening and Reading through the usual 40-question conversion
 * tables, then the mean of all skills.
 */
public final class PlacementBandCalculator {
    private static final int FULL_TEST_QUESTIONS = 40;
    /** Bands from 9.0 down to 1.0 in steps of 0.5; the entry at the same index of a table is the fewest correct answers (of 40) that earns it. */
    private static final double[] BANDS = {9.0, 8.5, 8.0, 7.5, 7.0, 6.5, 6.0, 5.5, 5.0, 4.5, 4.0, 3.5, 3.0, 2.5, 2.0, 1.5, 1.0};
    private static final int[] LISTENING_MIN_CORRECT = {39, 37, 35, 32, 30, 26, 23, 18, 16, 13, 10, 8, 6, 4, 3, 2, 1};
    private static final int[] READING_MIN_CORRECT = {39, 37, 35, 33, 30, 27, 23, 19, 15, 13, 10, 8, 6, 4, 3, 2, 1};

    private PlacementBandCalculator() {
    }

    /**
     * Band of Listening or Reading. The score is scaled to a 40-question test, so a shorter test converts the same way;
     * answering nothing correctly is band 0 and any correct answer earns at least band 1.
     */
    public static double bandForCorrect(Skill skill, int correct, int total) {
        if (skill != Skill.LISTENING && skill != Skill.READING) {
            throw new IllegalArgumentException("Only Listening and Reading are converted from correct answers");
        }
        if (correct <= 0 || total <= 0) {
            return 0.0;
        }
        int scaled = Math.max(1, (int) Math.round(Math.min(correct, total) * (double) FULL_TEST_QUESTIONS / total));
        int[] minCorrect = skill == Skill.LISTENING ? LISTENING_MIN_CORRECT : READING_MIN_CORRECT;
        for (int i = 0; i < minCorrect.length; i++) {
            if (scaled >= minCorrect[i]) {
                return BANDS[i];
            }
        }
        return 0.0;
    }

    /**
     * Weight of a Writing task in the Writing band: Task 2 counts twice as much as Task 1, as in the IELTS test. A task
     * that is not marked {@code TASK_2} counts once.
     */
    public static double writingTaskWeight(Object task) {
        return "TASK_2".equals(task) ? 2.0 : 1.0;
    }

    /** Mean of the skill bands, rounded to the nearest half band (a tie rounds up). */
    public static double overallBand(Collection<Double> skillBands) {
        if (skillBands == null || skillBands.isEmpty()) {
            throw new IllegalArgumentException("At least one skill band is required");
        }
        double mean = skillBands.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        return Math.floor(mean * 2 + 0.5) / 2;
    }
}
