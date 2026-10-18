package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.MasteryRules;
import com.ieltspath.learning.domain.vo.MasteryTier;

import java.time.Duration;
import java.time.Instant;

/** Turns a mastery score into a tier, a success streak and the next review date. The score itself is not computed here. */
public final class MasteryPolicy {
    private final MasteryRules rules;

    public MasteryPolicy(MasteryRules rules) {
        this.rules = rules;
    }

    /** The score is stored with three decimals, so the tier is cut on the stored value. */
    public static double stored(double mastery) {
        return Math.round(mastery * 1000.0) / 1000.0;
    }

    public MasteryTier tier(double mastery) {
        double value = stored(mastery);
        if (value < rules.priorityBelow()) return MasteryTier.PRIORITY;
        return value < rules.maintainFrom() ? MasteryTier.CONSOLIDATE : MasteryTier.MAINTAIN;
    }

    /**
     * A wrong answer resets the streak. A right one adds to it unless the tier fell; a right answer that still drops the
     * tier leaves the streak as it was. The first evidence starts at one when right.
     */
    public int nextStreak(Integer previousStreak, MasteryTier previousTier, MasteryTier newTier, boolean lastCorrect) {
        if (!lastCorrect) return 0;
        if (previousStreak == null || previousTier == null) return 1;
        return newTier.rank() >= previousTier.rank() ? previousStreak + 1 : previousStreak;
    }

    /**
     * MP-1.0 tier. Without enough evidence the point is {@link MasteryTier#INSUFFICIENT}. Otherwise the score is cut at
     * the thresholds, except that a point already judged keeps its tier until the score is past the threshold by
     * {@code hysteresis}: moving up needs {@code threshold + hysteresis}, moving down needs a score under
     * {@code threshold - hysteresis}. A point with no previous judgement, or an insufficient one, is cut plainly.
     */
    public MasteryTier tier(double mastery, boolean sufficient, MasteryTier previous, double hysteresis) {
        if (!sufficient) return MasteryTier.INSUFFICIENT;
        if (previous == null || previous == MasteryTier.INSUFFICIENT) return tier(mastery);
        long value = thousandths(stored(mastery));
        long margin = thousandths(hysteresis);
        // A point leaves its tier only past the threshold by the margin, so each threshold moves away from the tier held.
        long low = thousandths(rules.priorityBelow()) + (previous == MasteryTier.PRIORITY ? margin : -margin);
        long high = thousandths(rules.maintainFrom()) + (previous == MasteryTier.MAINTAIN ? -margin : margin);
        if (value < low) return MasteryTier.PRIORITY;
        return value >= high ? MasteryTier.MAINTAIN : MasteryTier.CONSOLIDATE;
    }

    private static long thousandths(double value) {
        return Math.round(value * 1000.0);
    }

    /** Null for {@link MasteryTier#PRIORITY} (a blocking review covers it) and for an insufficient point. */
    public Instant nextReviewAt(MasteryTier tier, Instant lastEvidenceAt, int successStreak) {
        return switch (tier) {
            case PRIORITY, INSUFFICIENT -> null;
            case CONSOLIDATE -> lastEvidenceAt.plus(Duration.ofDays(rules.consolidateDays()));
            case MAINTAIN -> {
                var intervals = rules.reviewIntervalsDays();
                yield lastEvidenceAt.plus(Duration.ofDays(intervals.get(Math.min(successStreak, intervals.size() - 1))));
            }
        };
    }
}
