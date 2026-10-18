package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.service.SkillRollup;
import com.ieltspath.learning.domain.vo.MasteryTier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Weekly and monthly snapshots of learners, and the batch reads that build them. Reads take a set of learners at once:
 * a snapshot run covers hundreds of learners and must not query them one by one.
 */
public interface SnapshotRepository {
    enum Period { WEEK, MONTH }

    /** {@code start} is the Monday of the week or the first day of the month. */
    record Snapshot(UUID userId, LocalDate start, List<SkillRollup.Summary> skills, Map<String, Integer> tierCounts,
                    int answered, int correct, int activeMinutes, List<UUID> focusKnowledgePointIds,
                    BigDecimal estimatedBand) {}

    /** What a learner answered in a period; {@code activeMs} caps each answer so an open question adds little. */
    record Activity(int answered, int correct, long activeMs) {}

    record ProfileRow(UUID knowledgePointId, MasteryTier tier, double mastery, int evidenceCount) {}

    /** Learners with at least one answer since {@code since}. */
    List<UUID> usersActiveSince(Instant since);

    Map<UUID, Activity> activity(Collection<UUID> userIds, Instant from, Instant to);

    Map<UUID, List<ProfileRow>> profiles(Collection<UUID> userIds);

    /** The placement band of each learner who has one. */
    Map<UUID, BigDecimal> placementBands(Collection<UUID> userIds);

    /** Inserts or replaces by learner and start, so a run can be repeated. */
    void save(Period period, List<Snapshot> snapshots);

    Optional<Snapshot> find(Period period, UUID userId, LocalDate start);

    /** Snapshots from {@code from} to {@code to} inclusive, oldest first. */
    List<Snapshot> range(Period period, UUID userId, LocalDate from, LocalDate to);

    /** Deletes snapshots that start before {@code cutoff}; returns how many. */
    int purgeBefore(Period period, LocalDate cutoff);
}
