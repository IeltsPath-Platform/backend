package com.ieltspath.assessment.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.result.AttemptStructureResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AttemptStructureResponse(List<Section> sections) {
    /** {@code startedAt} is null until the learner first opens the section, {@code completedAt} until they finish it. */
    public record Section(UUID id, UUID contentSectionId, int sortOrder, Snapshot snapshot, List<Item> items,
                          Instant startedAt, Instant completedAt) {}

    public record Snapshot(String title, String skill, String instructions,
                           @JsonInclude(JsonInclude.Include.NON_NULL) String passage,
                           @JsonInclude(JsonInclude.Include.NON_NULL) Audio audio) {
        private static Snapshot from(String raw, ObjectMapper json) {
            try {
                JsonNode value = raw == null ? null : json.readTree(raw);
                if (value == null || !value.isObject()) return empty();
                JsonNode audio = value.path("audio");
                JsonNode duration = audio.path("durationSeconds");
                return new Snapshot(text(value.path("title")),
                        value.path("skill").isMissingNode() ? "READING" : text(value.path("skill")),
                        text(value.path("instructions")), text(value.path("passage")),
                        audio.isObject() ? new Audio(text(audio.path("url")),
                                duration.isIntegralNumber() && duration.canConvertToInt() ? duration.intValue() : null)
                                : null);
            } catch (JsonProcessingException exception) {
                return empty();
            }
        }

        private static Snapshot empty() {
            return new Snapshot(null, null, null, null, null);
        }

        private static String text(JsonNode value) {
            return value.isTextual() ? value.textValue() : null;
        }
    }

    public record Audio(String url, Integer durationSeconds) {}

    public record Item(UUID id, UUID questionVersionId, int sortOrder,
                       String questionSnapshot, String knowledgeSnapshot) {}

    public static AttemptStructureResponse from(AttemptStructureResult result, ObjectMapper json) {
        return new AttemptStructureResponse(result.sections().stream()
                .map(section -> new Section(
                        section.id(),
                        section.contentSectionId(),
                        section.sortOrder(),
                        Snapshot.from(section.snapshot(), json),
                        section.items().stream()
                                .map(item -> new Item(
                                        item.id(),
                                        item.questionVersionId(),
                                        item.sortOrder(),
                                        item.questionSnapshot(),
                                        item.knowledgeSnapshot()))
                                .toList(),
                        section.startedAt(),
                        section.completedAt()))
                .toList());
    }
}
