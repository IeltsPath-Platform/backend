package com.ieltspath.assessment.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.event.OutboxEventsRecorded;
import com.ieltspath.assessment.domain.entity.OutboxEvent;
import com.ieltspath.assessment.domain.repository.OutboxEventRepository;
import com.ieltspath.events.assessment.GradingRequestedV1;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Tells the examiner, at once, that a learner chose them for a job. The notice is an outbox row written in the same
 * transaction that hands the job over, so a job that was handed over is always announced and a job that was not (the
 * transaction rolled back) never is. Grading is blind: the notice names the job, not the learner.
 */
@Component
public class GradingRequestAnnouncer {
    static final String AGGREGATE_TYPE = "GradingJob";

    private final OutboxEventRepository outbox;
    private final ObjectMapper json;
    private final ApplicationEventPublisher events;

    public GradingRequestAnnouncer(OutboxEventRepository outbox, ObjectMapper json, ApplicationEventPublisher events) {
        this.outbox = outbox;
        this.json = json;
        this.events = events;
    }

    /** @param chosenAt when the learner chose the examiner; the deadline is 48 hours after it. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void announce(UUID examinerId, UUID jobId, String skill, Instant chosenAt) {
        UUID eventId = UUID.randomUUID();
        var event = new GradingRequestedV1(eventId, GradingRequestedV1.EVENT_TYPE, chosenAt, examinerId, jobId, skill,
                chosenAt.plus(ExaminerAssignmentPolicy.DEADLINE));
        try {
            outbox.save(OutboxEvent.pending(eventId, AGGREGATE_TYPE, jobId.toString(), GradingRequestedV1.EVENT_TYPE,
                    json.writeValueAsString(event), chosenAt));
            // The examiner is waiting to hear this: once the transaction commits, publish it now.
            events.publishEvent(new OutboxEventsRecorded());
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("The grading request notice cannot be written", failure);
        }
    }
}
