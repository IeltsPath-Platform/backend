package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.vo.LearningSkill;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The review basket. {@code required} are the blocking reviews the learner must finish; {@code consolidate} and
 * {@code due} are optional: knowledge points at the consolidate tier, and maintained points whose review date has come.
 */
public record ReviewBasketResult(List<RequiredItem> required, List<OptionalItem> consolidate,
                                 List<OptionalItem> due) {
    public record RequiredItem(UUID reviewId, UUID knowledgePointId, String code, String name, UUID lessonId,
                               LearningSkill skill) {}

    /**
     * {@code reason} is {@code CONSOLIDATE} or {@code DUE}. {@code reviewId} is set when the learner already started
     * an optional review of the point. {@code hasUnrevealedSet} says whether Content still has a set the learner has
     * not seen; without one the point cannot be started.
     */
    public record OptionalItem(UUID knowledgePointId, String code, String name, String skill, String tier,
                               double mastery, Instant dueAt, String reason, boolean hasUnrevealedSet, UUID reviewId) {}
}
