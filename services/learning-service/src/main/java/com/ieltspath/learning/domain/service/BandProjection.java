package com.ieltspath.learning.domain.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Projects the date a learner reaches their target band from their weekly estimated bands: a straight line fitted to the
 * four most recent weeks. With fewer than four weeks, or a line that does not rise, there is no date and the reason says
 * why. A projection is a guide, not a promise.
 */
public final class BandProjection {
    public static final int WEEKS_FITTED = 4;
    private static final double TOLERANCE = 1e-9;

    private BandProjection() {}

    /** The estimated band of the week starting {@code weekStart}; null when no skill had a band that week. */
    public record WeekBand(LocalDate weekStart, BigDecimal band) {}

    /** Exactly one of {@code date} and {@code reason} is set: NOT_ENOUGH_DATA or NO_PROGRESS. */
    public record Projection(LocalDate date, String reason) {
        static Projection on(LocalDate date) {
            return new Projection(date, null);
        }

        static Projection because(String reason) {
            return new Projection(null, reason);
        }
    }

    public static Projection project(List<WeekBand> weeks, BigDecimal targetBand) {
        List<WeekBand> recent = weeks.stream().filter(week -> week.band() != null)
                .sorted(Comparator.comparing(WeekBand::weekStart)).toList();
        if (recent.size() < WEEKS_FITTED) return Projection.because("NOT_ENOUGH_DATA");
        recent = recent.subList(recent.size() - WEEKS_FITTED, recent.size());

        // Least squares over x = 0..3 (the position of the week among the four).
        double meanX = (WEEKS_FITTED - 1) / 2.0;
        double meanY = recent.stream().mapToDouble(week -> week.band().doubleValue()).average().orElseThrow();
        double covariance = 0;
        double variance = 0;
        for (int index = 0; index < WEEKS_FITTED; index++) {
            covariance += (index - meanX) * (recent.get(index).band().doubleValue() - meanY);
            variance += (index - meanX) * (index - meanX);
        }
        double slope = covariance / variance;
        double fittedLast = meanY + slope * (WEEKS_FITTED - 1 - meanX);
        LocalDate lastWeek = recent.get(WEEKS_FITTED - 1).weekStart();

        if (fittedLast >= targetBand.doubleValue() - TOLERANCE) return Projection.on(lastWeek);
        if (slope <= TOLERANCE) return Projection.because("NO_PROGRESS");
        long weeksNeeded = (long) Math.ceil((targetBand.doubleValue() - fittedLast) / slope - TOLERANCE);
        return Projection.on(lastWeek.plusWeeks(weeksNeeded));
    }
}
