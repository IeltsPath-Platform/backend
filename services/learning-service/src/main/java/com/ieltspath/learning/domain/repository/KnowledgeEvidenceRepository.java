package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import com.ieltspath.learning.domain.vo.MasteryHistory;

import java.util.List;
import java.util.UUID;

/** Mastery evidence in arrival order; a row already stored for the same source reference is kept. */
public interface KnowledgeEvidenceRepository {
    void append(UUID userId, List<KnowledgeEvidence> evidence);

    /** Removes the formal-result evidence of a regraded attempt; lesson, review and writing evidence stay. */
    void removeAssessmentEvidence(UUID userId, UUID attemptId);

    /** Every catalog knowledge point with the learner's history, including points without evidence. */
    List<MasteryHistory> findMasteryHistories(UUID userId);
}
