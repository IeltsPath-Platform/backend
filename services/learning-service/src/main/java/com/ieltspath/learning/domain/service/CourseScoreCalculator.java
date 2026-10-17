package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.CourseScoreConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The weighted course score. A component the learner has not done yet is 0; a component the course does not have (null)
 * is left out and its weight shared out among the others, both inside Process and in the total. Values are fractions
 * of 1.
 */
public final class CourseScoreCalculator {

    /** {@code null} means the course has no such component; 0 means the learner has not done any of it. */
    public record Components(Double practice, Double practiceTest, Double mid, Double finalTest) {}

    /**
     * {@code process}, {@code mid} and {@code finalTest} are null when the course has none of them; the three weights
     * are the ones actually applied, after sharing out a missing component, and add up to 1 (all 0 when nothing counts).
     * {@code total} is rounded to four decimals.
     */
    public record Score(Double process, Double mid, Double finalTest, double processWeight, double midWeight,
                        double finalWeight, double practiceShare, double practiceTestShare, double total) {}

    public Score calculate(CourseScoreConfig config, Components components) {
        Double process = process(config, components);
        double[] shares = shares(config, components);
        double[] weights = normalise(new double[] {config.processWeight().doubleValue(),
                config.midWeight().doubleValue(), config.finalWeight().doubleValue()},
                new boolean[] {process != null, components.mid() != null, components.finalTest() != null});
        double total = weights[0] * orZero(process) + weights[1] * orZero(components.mid())
                + weights[2] * orZero(components.finalTest());
        return new Score(process, components.mid(), components.finalTest(), weights[0], weights[1], weights[2],
                shares[0], shares[1], round(total));
    }

    /** A total passes when its four-decimal value reaches the threshold. */
    public boolean passes(CourseScoreConfig config, double total) {
        return BigDecimal.valueOf(total).compareTo(config.passThreshold()) >= 0;
    }

    private static Double process(CourseScoreConfig config, Components components) {
        double[] shares = shares(config, components);
        if (components.practice() == null && components.practiceTest() == null) return null;
        return shares[0] * orZero(components.practice()) + shares[1] * orZero(components.practiceTest());
    }

    private static double[] shares(CourseScoreConfig config, Components components) {
        return normalise(new double[] {config.practiceShare().doubleValue(), config.practiceTestShare().doubleValue()},
                new boolean[] {components.practice() != null, components.practiceTest() != null});
    }

    /** The weights of the present parts, scaled to add up to 1; the absent ones get 0. */
    private static double[] normalise(double[] weights, boolean[] present) {
        double sum = 0;
        for (int index = 0; index < weights.length; index++) if (present[index]) sum += weights[index];
        double[] applied = new double[weights.length];
        if (sum == 0) {
            // Every present part has weight 0, so share the weight equally rather than dividing by zero.
            int count = 0;
            for (boolean part : present) if (part) count++;
            for (int index = 0; index < weights.length; index++) applied[index] = present[index] ? 1.0 / count : 0;
            return applied;
        }
        for (int index = 0; index < weights.length; index++) applied[index] = present[index] ? weights[index] / sum : 0;
        return applied;
    }

    private static double orZero(Double value) {
        return value == null ? 0 : value;
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
