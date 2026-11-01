package com.ieltspath.learning.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Learner timezone, the XP ledger and the XP totals. */
public interface GamificationRepository {
    Optional<String> timezone(UUID userId);

    /** The saved timezone of each learner that chose one, read in one query. */
    java.util.Map<UUID, String> timezones(java.util.Collection<UUID> userIds);

    void saveTimezone(UUID userId, String timezone);

    /** Answer XP counted on the learner's given local day; milestone XP is not part of it. */
    long countedAnswerXpOn(UUID userId, LocalDate day);

    /**
     * Inserts ledger rows and says, per row, whether it was new. A row whose source and reference already exist is not
     * new: it is a repeat, so it never adds XP twice.
     */
    boolean[] insertLedger(UUID userId, List<LedgerEntry> entries);

    /** Counted XP of the learner; zero before any XP. */
    int totalXp(UUID userId);

    void saveTotals(UUID userId, int totalXp, int level);

    record LedgerEntry(String source, String sourceRef, int amount, boolean counted, LocalDate localDate) {}
}
