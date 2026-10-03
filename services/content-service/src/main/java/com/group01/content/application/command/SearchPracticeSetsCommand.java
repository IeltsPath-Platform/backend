package com.group01.content.application.command;

import java.util.List;
import java.util.UUID;

/**
 * Null {@code excludePackageIds}, {@code minQuestions} or {@code limit} take their defaults; a
 * {@code preferredLessonId} puts that lesson's practice sets first.
 */
public record SearchPracticeSetsCommand(
        UUID knowledgePointId,
        List<UUID> excludePackageIds,
        Integer minQuestions,
        Integer limit,
        UUID preferredLessonId
) {}
