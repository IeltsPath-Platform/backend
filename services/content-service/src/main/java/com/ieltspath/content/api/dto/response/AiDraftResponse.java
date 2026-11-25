package com.ieltspath.content.api.dto.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import com.ieltspath.content.application.result.AiDraftOrigin;

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
        String reviewNote,
        AiDraftOrigin origin,
        int itemCount
) {
    public static AiDraftResponse from(AiDraft draft, ObjectMapper json) {
        JsonNode result = parse(json, draft.getResultJson());
        questionMetadata(draft, result);
        return new AiDraftResponse(draft.getId(), draft.getKind(), draft.getStatus(),
                parse(json, draft.getRequestJson()), result,
                draft.getErrorCode(), draft.getModel(), draft.getPromptVersion(), draft.getCreatedAt(),
                draft.getFinishedAt(), draft.getReviewedAt(), draft.getReviewNote(), null, itemCount(result));
    }

    public static AiDraftResponse summary(AiDraft draft, ObjectMapper json) {
        return summary(draft, json, null);
    }

    public static AiDraftResponse summary(AiDraft draft, ObjectMapper json, AiDraftOrigin origin) {
        JsonNode request = parse(json, draft.getRequestJson());
        if (request != null && request.path("source").isObject()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) request.path("source")).remove("text");
        }
        JsonNode result = parse(json, draft.getResultJson());
        questionMetadata(draft, result);
        return new AiDraftResponse(draft.getId(), draft.getKind(), draft.getStatus(), request,
                result, draft.getErrorCode(), draft.getModel(), draft.getPromptVersion(),
                draft.getCreatedAt(), draft.getFinishedAt(), draft.getReviewedAt(), draft.getReviewNote(), origin, itemCount(result));
    }

    private static int itemCount(JsonNode result) {
        return result != null && result.path("items").isArray() ? result.path("items").size() : 0;
    }

    private static void questionMetadata(AiDraft draft, JsonNode result) {
        if (draft.getKind() != DraftKind.QUESTIONS || !(result instanceof com.fasterxml.jackson.databind.node.ObjectNode object)
                || !object.path("items").isArray()) return;
        if (!object.path("revision").canConvertToInt()) object.put("revision", 0);
        var items = (com.fasterxml.jackson.databind.node.ArrayNode) object.path("items");
        for (int index = 0; index < items.size(); index++) {
            var item = items.get(index);
            if (item.isObject() && !item.path("itemId").isTextual()) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) item).put("itemId", UUID.nameUUIDFromBytes(
                        (draft.getId() + ":" + index).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());
            }
        }
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
