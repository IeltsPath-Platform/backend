package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.MasteryTier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Estimates a learner's band from their mastery. A skill reaches a band when at least 80% of its knowledge points at that
 * band are maintained; with no band reached it takes the placement band. The overall band is the mean of the skills that
 * have one, rounded to a half. Grammar and vocabulary are not skills and never enter the band. This is an estimate, not
 * an exam result.
 */
public final class BandEstimator {
    /** The skills a band is estimated for. */
    public static final List<String> SKILLS = List.of("LISTENING", "READING", "WRITING", "SPEAKING");
    static final double ENOUGH = 0.8;
    static final int MAX_HINT_POINTS = 5;

    private BandEstimator() {}

    /** A knowledge point's skill, the band of its course, and how the learner stands on it. */
    public record Point(UUID id, String skill, BigDecimal band, MasteryTier tier, double mastery) {}

    /** The next band of a skill is within reach: the points still to be maintained there, weakest first. */
    public record Hint(String skill, BigDecimal nextBand, List<UUID> pointIds) {}

    /** {@code overall} is null when no skill has a band. */
    public record Estimate(BigDecimal overall, Map<String, BigDecimal> skillBands, boolean speakingIncluded,
                           List<Hint> hints) {}

    public static Estimate estimate(List<Point> points, BigDecimal placementBand) {
        Map<String, BigDecimal> bands = new LinkedHashMap<>();
        List<Hint> hints = new ArrayList<>();
        for (String skill : SKILLS) {
            TreeMap<BigDecimal, List<Point>> byBand = new TreeMap<>();
            for (Point point : points) {
                if (skill.equals(point.skill()) && point.band() != null) {
                    byBand.computeIfAbsent(point.band(), band -> new ArrayList<>()).add(point);
                }
            }
            if (byBand.isEmpty()) continue;
            BigDecimal reached = null;
            for (Map.Entry<BigDecimal, List<Point>> entry : byBand.entrySet()) {
                if (share(entry.getValue(), MasteryTier.MAINTAIN) >= ENOUGH) reached = entry.getKey();
            }
            BigDecimal skillBand = reached != null ? reached : placementBand;
            if (skillBand != null) bands.put(skill, skillBand);
            hint(skill, skillBand, byBand).ifPresent(hints::add);
        }
        return new Estimate(overall(bands), bands, bands.containsKey("SPEAKING"), hints);
    }

    private static java.util.Optional<Hint> hint(String skill, BigDecimal from, TreeMap<BigDecimal, List<Point>> byBand) {
        Map.Entry<BigDecimal, List<Point>> next = from == null ? byBand.firstEntry() : byBand.higherEntry(from);
        if (next == null || share(next.getValue(), MasteryTier.CONSOLIDATE) < ENOUGH) {
            return java.util.Optional.empty();
        }
        List<UUID> todo = next.getValue().stream().filter(point -> point.tier() != MasteryTier.MAINTAIN)
                .sorted(Comparator.comparingDouble(Point::mastery)).limit(MAX_HINT_POINTS).map(Point::id).toList();
        return todo.isEmpty() ? java.util.Optional.empty()
                : java.util.Optional.of(new Hint(skill, next.getKey(), todo));
    }

    /** Share of the points whose tier is at least {@code floor}; an insufficient point never reaches it. */
    private static double share(List<Point> points, MasteryTier floor) {
        long enough = points.stream().filter(point -> point.tier().rank() >= floor.rank()).count();
        return (double) enough / points.size();
    }

    private static BigDecimal overall(Map<String, BigDecimal> bands) {
        if (bands.isEmpty()) return null;
        BigDecimal sum = bands.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mean = sum.divide(BigDecimal.valueOf(bands.size()), 6, RoundingMode.HALF_UP);
        return mean.multiply(BigDecimal.valueOf(2)).setScale(0, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(2), 1, RoundingMode.UNNECESSARY);
    }
}
