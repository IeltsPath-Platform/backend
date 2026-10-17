package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.LeagueTier;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

/**
 * Weekly league rules. A group holds up to 30 learners of one tier. At the end of the week the top 7 move up a tier and,
 * in a group of at least 23, ranks 23 and below move down; the rest stay. A tier above the top or below the bottom stays
 * where it is. A league week runs from Sunday 00:00 UTC to the next Sunday 00:00 UTC.
 */
public final class LeagueRules {
    public static final int GROUP_SIZE = 30;
    public static final int PROMOTE_TOP = 7;
    public static final int DEMOTE_FROM = 23;

    private LeagueRules() {}

    public enum Outcome { PROMOTED, STAYED, DEMOTED }

    /** Where a rank stands in the group's table while the week is open. */
    public enum Zone { PROMOTION, SAFE, DEMOTION }

    public static Outcome outcome(int rank, int groupSize) {
        if (rank <= PROMOTE_TOP) return Outcome.PROMOTED;
        if (groupSize >= DEMOTE_FROM && rank >= DEMOTE_FROM) return Outcome.DEMOTED;
        return Outcome.STAYED;
    }

    public static Zone zone(int rank, int groupSize) {
        return switch (outcome(rank, groupSize)) {
            case PROMOTED -> Zone.PROMOTION;
            case DEMOTED -> Zone.DEMOTION;
            case STAYED -> Zone.SAFE;
        };
    }

    public static LeagueTier next(LeagueTier tier, Outcome outcome) {
        return switch (outcome) {
            case PROMOTED -> LeagueTier.ofLevel(Math.min(LeagueTier.HIGHEST, tier.level() + 1));
            case DEMOTED -> LeagueTier.ofLevel(Math.max(LeagueTier.LOWEST, tier.level() - 1));
            case STAYED -> tier;
        };
    }

    /** The Sunday that starts the league week {@code at} falls in, in UTC. */
    public static LocalDate weekStart(Instant at) {
        return at.atZone(ZoneOffset.UTC).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    /** The moment the week starting {@code weekStart} closes. */
    public static Instant closesAt(LocalDate weekStart) {
        return weekStart.plusDays(7).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
