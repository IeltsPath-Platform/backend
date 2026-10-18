package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;
import com.ieltspath.learning.domain.vo.QuestionEventSource;

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

    /**
     * The new answers the learner submitted in Learning (lesson, Practice, review, diagnostic). Assessment events are
     * left out: their result is finalised when grading ends, not when the learner answered, so they say nothing about
     * which day the learner studied.
     */
    public List<QuestionAttemptEvent> submittedAnswers() {
        return events.stream().filter(event -> event.source() != QuestionEventSource.ASSESSMENT).toList();
    }
}
