package com.group01.learning.domain.aggregate;

import java.time.Instant;
import java.util.*;

/**
 * A learner's progress in one lesson: where Content places it, which exercise blocks passed, and completion. A block
 * passes once; a lesson completes once and stays completed.
 */
public final class LessonProgress {
    private final UUID userId;
    private final UUID lessonId;
    private UUID topicId;
    private int sortOrder;
    private List<UUID> knowledgePointIds;
    private final Set<UUID> passedBlockIds;
    private Instant completedAt;

    private LessonProgress(UUID userId, UUID lessonId, UUID topicId, int sortOrder, List<UUID> knowledgePointIds,
                           Set<UUID> passedBlockIds, Instant completedAt) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.lessonId = Objects.requireNonNull(lessonId, "lessonId");
        this.topicId = Objects.requireNonNull(topicId, "topicId");
        this.sortOrder = sortOrder;
        this.knowledgePointIds = List.copyOf(knowledgePointIds);
        this.passedBlockIds = new LinkedHashSet<>(passedBlockIds);
        this.completedAt = completedAt;
    }

    public static LessonProgress start(UUID userId, UUID lessonId, UUID topicId, int sortOrder,
                                       List<UUID> knowledgePointIds) {
        return new LessonProgress(userId, lessonId, topicId, sortOrder, knowledgePointIds, Set.of(), null);
    }

    public static LessonProgress restore(UUID userId, UUID lessonId, UUID topicId, int sortOrder,
                                         List<UUID> knowledgePointIds, Set<UUID> passedBlockIds, Instant completedAt) {
        return new LessonProgress(userId, lessonId, topicId, sortOrder, knowledgePointIds, passedBlockIds, completedAt);
    }

    /** Follows Content: the lesson's topic, order and knowledge points as Content currently has them. */
    public void place(UUID topicId, int sortOrder, List<UUID> knowledgePointIds) {
        this.topicId = Objects.requireNonNull(topicId, "topicId");
        this.sortOrder = sortOrder;
        this.knowledgePointIds = List.copyOf(knowledgePointIds);
    }

    /** True when this call passed the block. */
    public boolean passBlock(UUID blockId) {
        return passedBlockIds.add(Objects.requireNonNull(blockId, "blockId"));
    }

    /** True when this call completed the lesson; completion happens once. */
    public boolean complete(Instant now) {
        if (completedAt != null) return false;
        completedAt = Objects.requireNonNull(now, "now");
        return true;
    }

    public boolean hasPassed(UUID blockId) { return passedBlockIds.contains(blockId); }
    public boolean isCompleted() { return completedAt != null; }

    public UUID userId() { return userId; }
    public UUID lessonId() { return lessonId; }
    public UUID topicId() { return topicId; }
    public int sortOrder() { return sortOrder; }
    public List<UUID> knowledgePointIds() { return knowledgePointIds; }
    public Set<UUID> passedBlockIds() { return Collections.unmodifiableSet(passedBlockIds); }
    public Instant completedAt() { return completedAt; }
}
