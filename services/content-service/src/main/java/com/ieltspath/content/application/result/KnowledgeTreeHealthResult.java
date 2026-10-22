package com.ieltspath.content.application.result;

import java.util.List;
import java.util.UUID;

/** Problems in the active part of the knowledge graph. All three lists are empty for a healthy graph. */
public record KnowledgeTreeHealthResult(List<UUID> isolatedIds, List<UUID> missingMetadataIds,
                                        List<LevelViolation> levelViolations) {
    /** An edge whose prerequisite has a higher CEFR level than the point that depends on it. */
    public record LevelViolation(UUID from, UUID to) {}
}
