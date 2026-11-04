package com.ieltspath.assessment.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.event.OutboxEventsRecorded;
import com.ieltspath.assessment.domain.entity.OutboxEvent;
import com.ieltspath.assessment.domain.repository.OutboxEventRepository;
import com.ieltspath.events.assessment.HumanReviewCompletedV1;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Tells Learning what the examiner graded in a standalone Writing review. The event is an outbox row written in the same
 * transaction that stores the review, so a stored review is always published and an unstored one never is. It carries the
 * criterion bands and the counted error codes only: no essay, comment or examiner identity.
 */
@Component
public class HumanReviewCompletedAnnouncer {
    static final String AGGREGATE_TYPE = "HumanReviewSubmission";

    private final OutboxEventRepository outbox;
    private final ObjectMapper json;
    private final ApplicationEventPublisher events;

    public HumanReviewCompletedAnnouncer(OutboxEventRepository outbox, ObjectMapper json,
                                         ApplicationEventPublisher events) {
        this.outbox = outbox;
        this.json = json;
        this.events = events;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void announce(UUID learnerId, UUID reviewSubmissionId, UUID sourceSubmissionId,
                         List<HumanReviewCompletedV1.Criterion> criteria,
                         List<HumanReviewCompletedV1.ErrorCodeCount> errorCodes, Instant occurredAt) {
        UUID eventId = UUID.randomUUID();
        var event = new HumanReviewCompletedV1(eventId, HumanReviewCompletedV1.EVENT_TYPE, occurredAt, learnerId,
                reviewSubmissionId, sourceSubmissionId, criteria, errorCodes);
        try {
            outbox.save(OutboxEvent.pending(eventId, AGGREGATE_TYPE, reviewSubmissionId.toString(),
                    HumanReviewCompletedV1.EVENT_TYPE, json.writeValueAsString(event), occurredAt));
            events.publishEvent(new OutboxEventsRecorded());
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("The human review completed event cannot be written", failure);
        }
    }
}
