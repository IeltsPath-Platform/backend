package com.ieltspath.content.api.dto.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;

import java.time.Instant;
import java.util.UUID;

/** A draft as its author sees it. The request and result are parsed so clients receive JSON objects, not strings. */
public record AiDraftResponse(
        UUID id,
        DraftKind kind,
        DraftStatus status,
        JsonNode request,
        JsonNode result,
        String errorCode,
        String model,
        String promptVersion,
        Instant createdAt,
        Instant finishedAt,
        Instant reviewedAt,
        String reviewNote
) {
    public static AiDraftResponse from(AiDraft draft, ObjectMapper json) {
        return new AiDraftResponse(draft.getId(), draft.getKind(), draft.getStatus(),
                parse(json, draft.getRequestJson()), parse(json, draft.getResultJson()),
                draft.getErrorCode(), draft.getModel(), draft.getPromptVersion(), draft.getCreatedAt(),
                draft.getFinishedAt(), draft.getReviewedAt(), draft.getReviewNote());
    }

    private static JsonNode parse(ObjectMapper json, String text) {
        if (text == null) {
            return null;
        }
        try {
            return json.readTree(text);
        } catch (JsonProcessingException invalid) {
            return null;
        }
    }
}
