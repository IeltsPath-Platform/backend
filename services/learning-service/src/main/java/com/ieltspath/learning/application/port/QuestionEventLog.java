package com.ieltspath.learning.application.port;

import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;

import java.util.List;
import java.util.UUID;

/**
 * Per-question event log. Written in the same transaction as the submission it describes. A row already stored for the
 * same source reference and question is kept, so a replayed submission adds nothing.
 */
public interface QuestionEventLog {
    void append(List<QuestionAttemptEvent> events);

    /** Removes the assessment events of one attempt, before a regraded result writes its own. */
    void removeAssessmentEvents(UUID userId, UUID attemptId);
}
