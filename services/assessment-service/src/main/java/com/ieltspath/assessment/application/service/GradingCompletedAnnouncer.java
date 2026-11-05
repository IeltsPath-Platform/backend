package com.ieltspath.assessment.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.event.OutboxEventsRecorded;
import com.ieltspath.assessment.domain.entity.OutboxEvent;
import com.ieltspath.assessment.domain.repository.OutboxEventRepository;
import com.ieltspath.events.assessment.GradingCompletedV1;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Tells the learner that the examiner finished grading their essay. The notice is an outbox row written in the same
 * transaction that stores the review, so a review that was stored is always announced and one that was not never is; once
 * the transaction commits the relay publishes it at once, so the bell rings as the examiner finishes. The notice carries no
 * band, comment or essay text: the learner reads those in the app.
 */
@Component
public class GradingCompletedAnnouncer {
    static final String AGGREGATE_TYPE = "GradingJob";

    private final OutboxEventRepository outbox;
    private final ObjectMapper json;
    private final ApplicationEventPublisher events;

    public GradingCompletedAnnouncer(OutboxEventRepository outbox, ObjectMapper json, ApplicationEventPublisher events) {
        this.outbox = outbox;
        this.json = json;
        this.events = events;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void announce(UUID learnerId, UUID gradingJobId, UUID reviewSubmissionId, String skill, Instant completedAt) {
        UUID eventId = UUID.randomUUID();
        var event = new GradingCompletedV1(eventId, GradingCompletedV1.EVENT_TYPE, completedAt, learnerId, gradingJobId,
                reviewSubmissionId, skill);
        try {
            outbox.save(OutboxEvent.pending(eventId, AGGREGATE_TYPE, gradingJobId.toString(), GradingCompletedV1.EVENT_TYPE,
                    json.writeValueAsString(event), completedAt));
            events.publishEvent(new OutboxEventsRecorded());
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("The grading completed notice cannot be written", failure);
        }
    }
}
