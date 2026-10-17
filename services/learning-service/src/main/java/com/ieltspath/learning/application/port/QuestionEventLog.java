package com.ieltspath.learning.application.port;

import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;
import com.ieltspath.learning.domain.vo.QuestionEventSource;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-question event log. Written in the same transaction as the submission it describes. A row already stored for the
 * same source reference and question is kept, so a replayed submission adds nothing.
 */
public interface QuestionEventLog {
    void append(List<QuestionAttemptEvent> events);

    /** Removes the assessment events of one attempt, before a regraded result writes its own. */
    void removeAssessmentEvents(UUID userId, UUID attemptId);

    /**
     * Time and error statistics per knowledge point from the events since {@code since}. A point without events is
     * absent. The median ignores events without a reported duration.
     */
    Map<UUID, EventStats> statsByKnowledgePoint(UUID userId, Collection<UUID> knowledgePointIds, Instant since);

    /** The latest {@code perPoint} events of each point, newest first. */
    Map<UUID, List<RecentEvent>> recentByKnowledgePoint(UUID userId, Collection<UUID> knowledgePointIds, int perPoint);

    /** {@code errors} are the most frequent error codes first, at most three. */
    record EventStats(Long medianDurationMs, List<ErrorCount> errors) {
        public EventStats {
            errors = List.copyOf(errors);
        }
    }

    record ErrorCount(String code, int count) {}

    /**
     * {@code revisionNo} and {@code revisionDelta} are set only for an essay event: the version number of the essay (1 for
     * the first) and its overall band minus the band of the version it revised (null for a first essay).
     */
    record RecentEvent(UUID questionVersionId, Boolean correct, Long durationMs, QuestionEventSource source,
                       String errorCode, Instant at, Integer revisionNo, java.math.BigDecimal revisionDelta) {
        public RecentEvent(UUID questionVersionId, Boolean correct, Long durationMs, QuestionEventSource source,
                           String errorCode, Instant at) {
            this(questionVersionId, correct, durationMs, source, errorCode, at, null, null);
        }
    }
}
