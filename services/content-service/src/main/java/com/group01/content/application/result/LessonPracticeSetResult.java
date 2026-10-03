package com.group01.content.application.result;

import java.util.List;
import java.util.UUID;

/**
 * A published practice set that belongs to a lesson's Practice. {@code knowledgePointIds} are the knowledge points
 * its questions measure; {@code requiredFeatureKey} is the Access feature needed for it, null when free.
 */
public record LessonPracticeSetResult(
        UUID lessonId,
        UUID packageId,
        UUID packageVersionId,
        String code,
        String title,
        int questionCount,
        List<UUID> knowledgePointIds,
        String requiredFeatureKey
) {}
