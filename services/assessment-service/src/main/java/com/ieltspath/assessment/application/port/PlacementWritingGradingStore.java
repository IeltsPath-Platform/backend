package com.ieltspath.assessment.application.port;

import com.ieltspath.events.assessment.PlacementWritingGradingCompletedV1;
import com.ieltspath.events.assessment.PlacementWritingGradingRequestedV1;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Hands the queued essay jobs of a placement attempt to Learning and records what comes back. */
public interface PlacementWritingGradingStore {
    record Essay(UUID attemptItemId, UUID questionVersionId, String text, String questionSnapshot,
                 String answerSnapshot, List<UUID> knowledgePointIds) { }

    /** The attempt's essays whose AI job is still waiting. */
    List<Essay> queued(UUID attemptId, UUID userId);

    /**
     * Marks the jobs of the event's essays as being graded and records the event for publishing. A job nobody answers
     * for goes back to the local grader once it has been in processing for too long.
     */
    void delegate(PlacementWritingGradingRequestedV1 event, Instant now);

    /**
     * Completes the graded essays; an essay Learning could not grade goes back to the local grader. An event already
     * applied changes nothing.
     */
    void record(UUID eventId, PlacementWritingGradingCompletedV1.Data result, Instant now);
}
