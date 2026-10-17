package com.ieltspath.learning.domain.vo;

/**
 * What a submission did to the learner's XP. {@code xpEarned} is the counted XP of this submission; {@code xpCapped} says
 * some answer went past the daily cap and was logged as shadow XP. {@code leveledUp} is true when the total crossed a
 * level in this submission.
 */
public record GamificationSummary(int xpEarned, boolean xpCapped, int totalXp, int level, boolean leveledUp) {}
