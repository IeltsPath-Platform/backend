package com.ieltspath.learning.domain.vo;

import java.time.Instant;

/**
 * What a submission did to the learner's XP and hearts. {@code xpEarned} is the counted XP of this submission;
 * {@code xpCapped} says some answer went past the daily cap and was logged as shadow XP; {@code leveledUp} is true when
 * the total crossed a level here. The heart fields are null for submissions that cost no hearts.
 */
public record GamificationSummary(int xpEarned, boolean xpCapped, int totalXp, int level, boolean leveledUp,
                                  Integer heartsLost, Integer heartsRemaining, Instant nextHeartAt) {
    public GamificationSummary(int xpEarned, boolean xpCapped, int totalXp, int level, boolean leveledUp) {
        this(xpEarned, xpCapped, totalXp, level, leveledUp, null, null, null);
    }

    /** The same XP summary with what the submission did to the hearts. */
    public GamificationSummary withHearts(int lost, int remaining, Instant nextAt) {
        return new GamificationSummary(xpEarned, xpCapped, totalXp, level, leveledUp, lost, remaining, nextAt);
    }
}
