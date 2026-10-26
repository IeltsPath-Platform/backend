package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.exception.LessonExerciseLockedException;
import com.ieltspath.content.domain.vo.LessonStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Lesson {
    private final UUID id;
    private final UUID topicId;
    private final String code;
    private String title;
    private String summary;
    private String summaryMarkdown;
    private Integer estimatedMinutes;
    private int sortOrder;
    private LessonStatus status;
    private Instant updatedAt;
    private final java.util.ArrayList<LessonBlock> blocks;
    private final java.util.ArrayList<UUID> knowledgePointIds;

    public Lesson(UUID id, UUID topicId, String code, String title, String summary, String summaryMarkdown,
                  Integer estimatedMinutes, int sortOrder, LessonStatus status, Instant updatedAt,
                  List<LessonBlock> blocks, List<UUID> knowledgePointIds) {
        this.id = Objects.requireNonNull(id);
        this.topicId = Objects.requireNonNull(topicId);
        this.code = Objects.requireNonNull(code);
        this.title = requireTitle(title);
        this.summary = summary;
        this.summaryMarkdown = summaryMarkdown;
        this.estimatedMinutes = requireMinutes(estimatedMinutes);
        this.sortOrder = requireOrder(sortOrder);
        this.status = Objects.requireNonNull(status);
        this.updatedAt = updatedAt == null ? Instant.now() : updatedAt;
        this.blocks = new java.util.ArrayList<>(blocks == null ? List.of() : blocks);
        this.knowledgePointIds = new java.util.ArrayList<>(knowledgePointIds == null ? List.of() : knowledgePointIds);
    }

    public void update(String title, String summary, String summaryMarkdown, Integer estimatedMinutes) {
        this.title = requireTitle(title);
        this.summary = summary;
        this.summaryMarkdown = summaryMarkdown;
        this.estimatedMinutes = requireMinutes(estimatedMinutes);
        updatedAt = Instant.now();
    }

    public void publish() {
        status = LessonStatus.PUBLISHED;
        updatedAt = Instant.now();
    }

    public void archive() {
        status = LessonStatus.ARCHIVED;
        updatedAt = Instant.now();
    }

    public void ensureExerciseEditable() {
        if (status != LessonStatus.DRAFT) throw new LessonExerciseLockedException();
    }

    public void replaceKnowledgePoints(List<UUID> ids) {
        knowledgePointIds.clear();
        knowledgePointIds.addAll(ids);
        updatedAt = Instant.now();
    }

    public void addBlock(LessonBlock block) {
        if (!id.equals(block.getLessonId())) throw new IllegalArgumentException("Block belongs to a different lesson");
        blocks.add(block);
        updatedAt = Instant.now();
    }

    public LessonBlock requireBlock(UUID blockId) {
        return blocks.stream().filter(block -> block.getId().equals(blockId)).findFirst()
                .orElseThrow(() -> new com.ieltspath.content.domain.exception.LessonBlockInvalidException("Block does not belong to lesson"));
    }

    public void removeBlock(UUID blockId) {
        blocks.remove(requireBlock(blockId));
        updatedAt = Instant.now();
    }

    public void reorderBlocks(List<UUID> blockIds) {
        if (blocks.size() != blockIds.size() || blockIds.stream().distinct().count() != blockIds.size()) {
            throw new com.ieltspath.content.domain.exception.LessonSortConflictException();
        }
        java.util.Map<UUID, LessonBlock> byId = new java.util.HashMap<>();
        blocks.forEach(block -> byId.put(block.getId(), block));
        if (!byId.keySet().equals(new java.util.HashSet<>(blockIds))) {
            throw new com.ieltspath.content.domain.exception.LessonSortConflictException();
        }
        blocks.clear();
        for (int index = 0; index < blockIds.size(); index++) {
            LessonBlock block = byId.get(blockIds.get(index));
            block.update(index, block.getTextContent(), block.getAssetId(), block.getVocabularySenseIds(),
                    block.getQuestionVersionIds(), block.getKnowledgePointIds());
            blocks.add(block);
        }
        updatedAt = Instant.now();
    }

    public void reorder(int newSortOrder) {
        this.sortOrder = requireOrder(newSortOrder);
        updatedAt = Instant.now();
    }


    private static String requireTitle(String value) {
        if (value == null || value.isBlank() || value.length() > 255) throw new IllegalArgumentException("Invalid lesson title");
        return value.strip();
    }

    private static Integer requireMinutes(Integer value) {
        if (value != null && value <= 0) throw new IllegalArgumentException("estimatedMinutes must be positive");
        return value;
    }

    private static int requireOrder(int value) {
        if (value < 0) throw new IllegalArgumentException("sortOrder must be non-negative");
        return value;
    }

    public UUID getId() { return id; }
    public UUID getTopicId() { return topicId; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getSummaryMarkdown() { return summaryMarkdown; }
    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public int getSortOrder() { return sortOrder; }
    public LessonStatus getStatus() { return status; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<LessonBlock> getBlocks() { return List.copyOf(blocks); }
    public List<UUID> getKnowledgePointIds() { return List.copyOf(knowledgePointIds); }
}
