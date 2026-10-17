package com.ieltspath.learning.domain.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** League groups, members and the tier each learner plays in. All writes run inside the caller's transaction. */
public interface LeagueRepository {
    /** A member of a group's table; {@code rank} counts from 1 by weekly XP, earlier-reached XP first on a tie. */
    record Member(UUID userId, int weeklyXp, int rank) {}

    /** One week a learner played; the rank and outcome are null until the week closes. */
    record HistoryRow(LocalDate weekStart, int tier, int weeklyXp, Integer finalRank, String outcome) {}

    /** The tier the learner plays in; empty before they have ever been placed. */
    Optional<Integer> tierOf(UUID userId);

    /** Adds XP to a learner already in a group this week; false when they are not in one yet. */
    boolean addXp(UUID userId, LocalDate week, int xp, Instant now);

    /**
     * Places a learner in a group of their tier, opening a new group when every one is full. Callers hold the
     * learner's lock; this takes a lock per week and tier so two learners cannot fill the thirty-first place.
     */
    void join(UUID userId, LocalDate week, int tier, int xp, Instant now);

    /** The learner's group this week as a ranked table; empty when they have no XP this week. */
    List<Member> table(UUID userId, LocalDate week);

    /** The tier of the learner's group this week. */
    Optional<Integer> weekTier(UUID userId, LocalDate week);

    List<HistoryRow> history(UUID userId, int weeks);

    /** League weeks that started before {@code current} and are not closed. */
    List<LocalDate> unclosedWeeksBefore(LocalDate current);

    /**
     * Closes one week: ranks every group, sets each member's outcome, and moves every learner to the tier they play in
     * next. Returns the number of members ranked, or -1 when the week was already closed.
     */
    int closeWeek(LocalDate week, int promoteTop, int demoteFrom, Instant now);
}
