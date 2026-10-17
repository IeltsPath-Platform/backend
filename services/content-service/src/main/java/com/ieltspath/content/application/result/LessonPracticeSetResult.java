package com.ieltspath.content.application.result;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import com.ieltspath.content.domain.vo.Skill;

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
        String requiredFeatureKey,
        List<Skill> skills,
        BigDecimal band
) {
    public LessonPracticeSetResult(UUID lessonId, UUID packageId, UUID packageVersionId, String code, String title,
                                   int questionCount, List<UUID> knowledgePointIds, String requiredFeatureKey) {
        this(lessonId, packageId, packageVersionId, code, title, questionCount, knowledgePointIds,
                requiredFeatureKey, List.of(), null);
    }

    public LessonPracticeSetResult(UUID lessonId, UUID packageId, UUID packageVersionId, String code, String title,
                                   int questionCount, List<UUID> knowledgePointIds, String requiredFeatureKey,
                                   List<Skill> skills) {
        this(lessonId, packageId, packageVersionId, code, title, questionCount, knowledgePointIds,
                requiredFeatureKey, skills, null);
    }
}
