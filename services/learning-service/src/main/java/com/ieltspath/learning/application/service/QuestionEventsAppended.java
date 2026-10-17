package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;

import java.util.List;
import java.util.UUID;

/**
 * Published when question events of a learner were written; {@link FlagDetector} looks at that learner once it commits.
 * {@code events} are the answers that were new in this write: a replayed submission has none.
 */
public record QuestionEventsAppended(UUID userId, List<QuestionAttemptEvent> events) {
    public QuestionEventsAppended {
        events = List.copyOf(events);
    }

    public QuestionEventsAppended(UUID userId) {
        this(userId, List.of());
    }
}
