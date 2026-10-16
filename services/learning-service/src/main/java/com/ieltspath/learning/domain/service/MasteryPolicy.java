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
        return newTier.ordinal() >= previousTier.ordinal() ? previousStreak + 1 : previousStreak;
    }

    /** Null for {@link MasteryTier#PRIORITY}: a blocking review already covers it. */
    public Instant nextReviewAt(MasteryTier tier, Instant lastEvidenceAt, int successStreak) {
        return switch (tier) {
            case PRIORITY -> null;
            case CONSOLIDATE -> lastEvidenceAt.plus(Duration.ofDays(rules.consolidateDays()));
            case MAINTAIN -> {
                var intervals = rules.reviewIntervalsDays();
                yield lastEvidenceAt.plus(Duration.ofDays(intervals.get(Math.min(successStreak, intervals.size() - 1))));
            }
        };
    }
}
