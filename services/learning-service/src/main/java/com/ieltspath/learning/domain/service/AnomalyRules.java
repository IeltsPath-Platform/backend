package com.ieltspath.learning.domain.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/** Rules that spot learner behaviour worth a staff look. They only raise a flag; staff decide what it means. */
public final class AnomalyRules {
    /** One answered question; {@code durationMs} and {@code correct} are null when not reported or not graded. */
    public record Sample(Long durationMs, Boolean correct) {}

    public record Thresholds(int minQuestions, long medianBelowMs, double minAccuracy, int burstCount) {}

    /**
     * Within one day: at least {@code minQuestions} answers with a duration, a median duration below the limit and an
     * accuracy at or above the minimum, counted over those same answers.
     */
    public boolean tooFastHighScore(List<Sample> daySamples, Thresholds limits) {
        List<Sample> timed = daySamples.stream().filter(sample -> sample.durationMs() != null
                && sample.correct() != null).toList();
        if (timed.size() < limits.minQuestions()) return false;
        List<Long> durations = timed.stream().map(Sample::durationMs).sorted().toList();
        int middle = durations.size() / 2;
        double median = durations.size() % 2 == 1 ? durations.get(middle)
                : (durations.get(middle - 1) + durations.get(middle)) / 2.0;
        double accuracy = (double) timed.stream().filter(Sample::correct).count() / timed.size();
        return median < limits.medianBelowMs() && accuracy >= limits.minAccuracy();
    }

    /** More than {@code burstCount} submissions inside the window the caller counted. */
    public boolean burst(long submissionsInWindow, Thresholds limits) {
        return submissionsInWindow > limits.burstCount();
    }

    /** A learner is inactive when the last answer is {@code days} days old or older. */
    public static boolean inactive(Instant lastAnswerAt, Instant now, int days) {
        return !lastAnswerAt.isAfter(inactiveCutoff(now, days));
    }

    /** The last answer at or before this instant makes a learner inactive. */
    public static Instant inactiveCutoff(Instant now, int days) {
        return now.minus(Duration.ofDays(days));
    }

    /** SHA-256 of the essay with all whitespace removed and letters lower-cased: copies differing only in spacing match. */
    public static String essayHash(String essay) {
        String normalised = essay.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(normalised.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
