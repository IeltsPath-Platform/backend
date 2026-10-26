package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.result.KnowledgeTreeHealthResult;

import java.util.List;
import java.util.UUID;

public record KnowledgeTreeHealthResponse(List<UUID> isolatedIds, List<UUID> missingMetadataIds,
                                          List<LevelViolation> levelViolations) {
    public record LevelViolation(UUID from, UUID to) {}

    public static KnowledgeTreeHealthResponse from(KnowledgeTreeHealthResult result) {
        return new KnowledgeTreeHealthResponse(result.isolatedIds(), result.missingMetadataIds(),
                result.levelViolations().stream().map(item -> new LevelViolation(item.from(), item.to())).toList());
    }
}
