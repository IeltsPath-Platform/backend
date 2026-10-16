package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import com.ieltspath.learning.domain.vo.MasteryHistory;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Mastery evidence in arrival order; a row already stored for the same source reference is kept. */
public interface KnowledgeEvidenceRepository {
    void append(UUID userId, List<KnowledgeEvidence> evidence);

    /**
     * Removes the formal-result evidence of a regraded attempt; lesson, review and writing evidence stay.
     *
     * @return the knowledge points that lost evidence, so their profiles can be rebuilt
     */
    Set<UUID> removeAssessmentEvidence(UUID userId, UUID attemptId);

    /** Every catalog knowledge point with the learner's history, including points without evidence. */
    List<MasteryHistory> findMasteryHistories(UUID userId);

    /** The same history for only the given catalog knowledge points. */
    List<MasteryHistory> findMasteryHistories(UUID userId, Collection<UUID> knowledgePointIds);

    /** When each of the given knowledge points last received evidence; points without evidence are absent. */
    Map<UUID, Instant> lastEvidenceAt(UUID userId, Collection<UUID> knowledgePointIds);
}
