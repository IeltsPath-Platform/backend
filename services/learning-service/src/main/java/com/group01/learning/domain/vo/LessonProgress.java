package com.group01.learning.domain.vo;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record LessonProgress(UUID lessonId, UUID topicId, int sortOrder,
                             List<UUID> knowledgePointIds, Set<UUID> passedBlockIds, Instant completedAt) {
    public LessonProgress {
        Objects.requireNonNull(lessonId, "lessonId");
        Objects.requireNonNull(topicId, "topicId");
        knowledgePointIds = List.copyOf(knowledgePointIds);
        passedBlockIds = Set.copyOf(passedBlockIds);
    }
}
