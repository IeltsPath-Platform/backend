package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.EvidencePoint;
import com.ieltspath.learning.domain.vo.Mp1Params;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * MP-1.0 mastery: a weighted share of right answers over the newest valid answers, where weight falls with the source
 * (practice and AI-graded essays count less than exams and reviews) and halves with age. It also says whether the
 * evidence is enough to judge the point at all. {@link MasteryCalculator} stays as the other formula.
 */
public final class Mp1MasteryCalculator {
    private static final double SECONDS_PER_DAY = 86400.0;

    /** {@code confidence} is the decayed weight of the answers in the window; {@code mastery} is 0 with none. */
    public record Result(double mastery, double confidence, boolean sufficient) {}

    private final Mp1Params params;

    public Mp1MasteryCalculator(Mp1Params params) {
        this.params = params;
    }

    /** Points must be in chronological order, oldest first. */
    public Result compute(List<EvidencePoint> points, Instant now) {
        List<EvidencePoint> window = window(withoutRepeats(points), now);
        if (window.isEmpty()) return new Result(0.0, 0.0, false);

        double earned = 0.0;
        double total = 0.0;
        Set<java.time.LocalDate> days = new HashSet<>();
        for (EvidencePoint point : window) {
            double weight = weight(point, now);
            total += weight;
            if (point.correct()) earned += weight;
            days.add(point.occurredAt().atOffset(ZoneOffset.UTC).toLocalDate());
        }
        double mastery = total == 0.0 ? 0.0 : earned / total;
        boolean sufficient = window.size() >= params.minEvidence() && days.size() >= params.minDistinctDays()
                && total >= params.minConfidence();
        return new Result(mastery, total, sufficient);
    }

    private double weight(EvidencePoint point, Instant now) {
        double ageDays = Math.max(0.0, Duration.between(point.occurredAt(), now).toMillis() / 1000.0 / SECONDS_PER_DAY);
        return params.weights().of(point.source()) * Math.pow(0.5, ageDays / params.halfLifeDays());
    }

    /** The same question inside the repeat window counts once: the first answer stands, later ones are dropped. */
    private List<EvidencePoint> withoutRepeats(List<EvidencePoint> points) {
        Duration repeat = Duration.ofDays(params.sameItemDays());
        Map<UUID, Instant> lastCounted = new HashMap<>();
        List<EvidencePoint> kept = new ArrayList<>();
        for (EvidencePoint point : points) {
            UUID question = point.questionVersionId();
            if (question != null) {
                Instant previous = lastCounted.get(question);
                if (previous != null && Duration.between(previous, point.occurredAt()).compareTo(repeat) < 0) continue;
                lastCounted.put(question, point.occurredAt());
            }
            kept.add(point);
        }
        return kept;
    }

    /** The newest {@code window} answers, passing over practice answers beyond the cap. Result is oldest first. */
    private List<EvidencePoint> window(List<EvidencePoint> points, Instant now) {
        List<EvidencePoint> taken = new ArrayList<>();
        int practice = 0;
        for (int index = points.size() - 1; index >= 0 && taken.size() < params.window(); index--) {
            EvidencePoint point = points.get(index);
            if (point.occurredAt().isAfter(now)) continue;
            if (Mp1Params.isPractice(point.source())) {
                if (practice >= params.maxPracticeInWindow()) continue;
                practice++;
            }
            taken.add(0, point);
        }
        return taken;
    }
}
