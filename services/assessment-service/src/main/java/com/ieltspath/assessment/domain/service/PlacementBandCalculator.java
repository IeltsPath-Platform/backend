package com.ieltspath.assessment.domain.service;

import java.util.Collection;

/** Turns placement results into IELTS-style bands: objective skills by percent correct, then the mean of all skills. */
public final class PlacementBandCalculator {
    private PlacementBandCalculator() {
    }

    /** Band of an objective skill from its percent of correct answers (0-100). */
    public static double bandForPercent(double percent) {
        if (percent < 30) return 4.0;
        if (percent < 45) return 4.5;
        if (percent < 60) return 5.0;
        if (percent < 70) return 5.5;
        if (percent < 80) return 6.0;
        if (percent < 90) return 6.5;
        return 7.0;
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
