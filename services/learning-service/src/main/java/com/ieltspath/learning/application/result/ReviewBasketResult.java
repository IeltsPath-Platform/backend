package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.vo.LearningSkill;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The review basket. {@code required} are the blocking reviews the learner must finish; {@code consolidate} and
 * {@code due} are optional: knowledge points at the consolidate tier, and maintained points whose review date has come.
 * {@code weak} lists other points with a pending optional review, including Writing criterion points at PRIORITY.
 */
public record ReviewBasketResult(List<RequiredItem> required, List<OptionalItem> consolidate,
                                 List<OptionalItem> due, List<OptionalItem> weak) {
    public ReviewBasketResult(List<RequiredItem> required, List<OptionalItem> consolidate, List<OptionalItem> due) {
        this(required, consolidate, due, List.of());
    }

    public ReviewBasketResult {
        required = List.copyOf(required);
        consolidate = List.copyOf(consolidate);
        due = List.copyOf(due);
        weak = List.copyOf(weak);
    }
    public record RequiredItem(UUID reviewId, UUID knowledgePointId, String code, String name, UUID lessonId,
                               LearningSkill skill) {}

    /**
     * {@code reason} is {@code CONSOLIDATE}, {@code DUE}, or the pending review's trigger kind in {@code weak}.
     * {@code reviewId} is set when the learner already started
     * an optional review of the point. {@code hasUnrevealedSet} says whether Content still has a set the learner has
     * not seen; without one the point cannot be started.
     */
    public record OptionalItem(UUID knowledgePointId, String code, String name, String skill, String tier,
                               double mastery, Instant dueAt, String reason, boolean hasUnrevealedSet, UUID reviewId) {}
}
