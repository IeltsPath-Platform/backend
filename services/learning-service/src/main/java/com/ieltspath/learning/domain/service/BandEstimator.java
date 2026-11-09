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
 * Estimates a learner's band from their mastery. A band is only judged once the learner has studied at least
 * {@value #MIN_COVERAGE_PERCENT}% of the catalog's points at that band, so a few lucky points cannot reach it. A skill
 * reaches a band when at least 80% of the points studied at that band are maintained and every judged band below it is
 * reached too; with no band reached it takes the placement band. The overall band is the mean of the skills that have
 * one, rounded to a half, and only exists once at least {@value #MIN_SKILLS} skills have a band. Grammar and vocabulary
 * are not skills and never enter the band. This is an estimate, not an exam result.
 */
public final class BandEstimator {
    /** The skills a band is estimated for. */
    public static final List<String> SKILLS = List.of("LISTENING", "READING", "WRITING", "SPEAKING");
    static final double ENOUGH = 0.8;
    static final int MAX_HINT_POINTS = 5;
    /** Share of a band's catalog points the learner must have studied before the band is judged. */
    static final int MIN_COVERAGE_PERCENT = 30;
    /** Skills that must have a band before an overall band is given. */
    static final int MIN_SKILLS = 2;

    private BandEstimator() {}

    /** How many points the catalog lists per skill and band: the denominator of the coverage a band needs. */
    public static final class Catalog {
        private final Map<String, TreeMap<BigDecimal, Integer>> counts = new java.util.HashMap<>();

        public Catalog add(String skill, BigDecimal band) {
            if (skill == null || band == null) return this;
            counts.computeIfAbsent(skill.toUpperCase(java.util.Locale.ROOT), key -> new TreeMap<>()).merge(band, 1, Integer::sum);
            return this;
        }

        int size(String skill, BigDecimal band) {
            TreeMap<BigDecimal, Integer> bands = counts.get(skill);
            return bands == null ? 0 : bands.getOrDefault(band, 0);
        }
    }

    /** A knowledge point's skill, the band of its course, and how the learner stands on it. */
    public record Point(UUID id, String skill, BigDecimal band, MasteryTier tier, double mastery) {}

    /** The next band of a skill is within reach: the points still to be maintained there, weakest first. */
    public record Hint(String skill, BigDecimal nextBand, List<UUID> pointIds) {}

    /** {@code overall} is null when no skill has a band. */
    public record Estimate(BigDecimal overall, Map<String, BigDecimal> skillBands, boolean speakingIncluded,
                           List<Hint> hints) {}

    public static Estimate estimate(List<Point> points, BigDecimal placementBand, Catalog catalog) {
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
                int studied = entry.getValue().size();
                int listed = Math.max(catalog.size(skill, entry.getKey()), studied);
                if ((long) studied * 100 < (long) listed * MIN_COVERAGE_PERCENT) continue;
                if (share(entry.getValue(), MasteryTier.MAINTAIN) < ENOUGH) break;
                reached = entry.getKey();
            }
            BigDecimal skillBand = reached != null ? reached : placementBand;
            if (skillBand != null) bands.put(skill, skillBand);
            hint(skill, skillBand, byBand).ifPresent(hints::add);
        }
        return new Estimate(bands.size() < MIN_SKILLS ? null : overall(bands), bands, bands.containsKey("SPEAKING"), hints);
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
