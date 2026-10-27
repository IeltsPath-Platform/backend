package com.ieltspath.assessment.api.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltspath.assessment.application.port.HumanReviewStore;

import java.time.Instant;

/** The examiner's own unfinished review. Never part of what a learner reads. */
public record HumanReviewDraftResponse(JsonNode payload, Instant savedAt) {
    public static HumanReviewDraftResponse from(HumanReviewStore.Draft draft) {
        return new HumanReviewDraftResponse(draft.payload(), draft.savedAt());
    }
}
