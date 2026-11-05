package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.aggregate.MasteryProfile;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface MasteryProfileRepository {
    /** The stored profiles of the given points; points without a profile are absent. */
    Map<UUID, MasteryProfile> find(UUID userId, Collection<UUID> knowledgePointIds);

    List<MasteryProfile> findAll(UUID userId);

    /**
     * How many of the learner's maintained points have reached their review date at {@code now}, counted where
     * {@link #findAll} lists them: error-trap points are left out, and so are the {@code excludedKnowledgePointIds}.
     */
    int countDue(UUID userId, Instant now, Collection<UUID> excludedKnowledgePointIds);

    /** Stored profiles for error-trap knowledge points, kept separate from learner aggregates. */
    List<MasteryProfile> findErrorTrapProfiles(UUID userId);

    /** Inserts or replaces by learner and knowledge point. */
    void upsert(List<MasteryProfile> profiles);

    /**
     * Writes only the tier and review date of each profile, and only while the stored score, evidence count and
     * streak still match: a profile that new evidence changed meanwhile is already on the current rules and is kept.
     */
    void reclassify(List<MasteryProfile> profiles);

    /** Removes the profiles of points that no longer have evidence. */
    void delete(UUID userId, Collection<UUID> knowledgePointIds);

    /** Learners holding evidence for a point that has no profile yet. */
    List<UUID> usersWithUnprofiledEvidence();

    Set<UUID> unprofiledKnowledgePoints(UUID userId);

    /** Profiles after the given key in (learner, knowledge point) order; a null key starts from the beginning. */
    List<MasteryProfile> findBatchAfter(UUID afterUserId, UUID afterKnowledgePointId, int limit);

    /** Like {@link #findBatchAfter} but only profiles whose newest evidence is older than {@code staleBefore}. */
    List<MasteryProfile> findStaleBatchAfter(java.time.Instant staleBefore, UUID afterUserId,
                                             UUID afterKnowledgePointId, int limit);
}
