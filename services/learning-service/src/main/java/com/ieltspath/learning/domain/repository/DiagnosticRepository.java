package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.vo.DiagnosticProbe;
import com.ieltspath.learning.domain.vo.DiagnosticSession;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface DiagnosticRepository {
    void insertSession(DiagnosticSession session);

    void insertProbe(DiagnosticProbe probe);

    Optional<DiagnosticSession> findSession(UUID userId, UUID sessionId);

    Optional<DiagnosticSession> findByRootReview(UUID reviewId);

    /** In creation order. */
    List<DiagnosticProbe> findProbes(UUID sessionId);

    Optional<DiagnosticProbe> findProbe(UUID sessionId, UUID probeId);

    void answerProbe(UUID probeId, int correct, int total, boolean passed, UUID requestId);

    /** Gives an OPEN probe another set, or none ({@code packageId} null) which turns it into NO_SET. */
    void replaceProbeSet(UUID probeId, UUID packageId, UUID packageVersionId);

    void conclude(UUID sessionId, Collection<UUID> knowledgePointIds);

    /** Root points of the learner's sessions that are still PROBING. */
    Set<UUID> probingRootKnowledgePoints(UUID userId);

    /** The ids of the reviews {@code reviewId} still waits for: those that are PENDING. */
    List<UUID> pendingWaitingOn(UUID reviewId);

    void waitOn(UUID reviewId, Collection<UUID> reviewIds);

    /** Counts one more time a review ran out of unseen sets for the point. */
    void recordContentGap(UUID knowledgePointId);
}
