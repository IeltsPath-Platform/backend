package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.exception.LessonBlockInvalidException;
import com.ieltspath.content.domain.vo.BlockType;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class LessonBlock {
    private final UUID id;
    private final UUID lessonId;
    private final BlockType type;
    private int sortOrder;
    private String textContent;
    private UUID assetId;
    private List<UUID> vocabularySenseIds;
    private List<UUID> questionVersionIds;
    private List<UUID> knowledgePointIds;

    public LessonBlock(UUID id, UUID lessonId, BlockType type, int sortOrder, String textContent, UUID assetId,
                       List<UUID> vocabularySenseIds, List<UUID> questionVersionIds, List<UUID> knowledgePointIds) {
        this.id = Objects.requireNonNull(id);
        this.lessonId = Objects.requireNonNull(lessonId);
        this.type = Objects.requireNonNull(type);
        // A stored block keeps whatever position it has; seeded vocabulary blocks sit at negative positions.
        this.sortOrder = sortOrder;
        applyPayload(textContent, assetId, vocabularySenseIds, questionVersionIds, knowledgePointIds);
    }

    public static LessonBlock create(UUID lessonId, BlockType type, int sortOrder, String textContent, UUID assetId,
                                     List<UUID> vocabularySenseIds, List<UUID> questionVersionIds,
                                     List<UUID> knowledgePointIds) {
        return new LessonBlock(UUID.randomUUID(), lessonId, type, requireOrder(sortOrder), textContent, assetId,
                vocabularySenseIds, questionVersionIds, knowledgePointIds);
    }

    /** A null {@code sortOrder} keeps the block where it is. */
    public void update(Integer sortOrder, String textContent, UUID assetId, List<UUID> vocabularySenseIds,
                       List<UUID> questionVersionIds, List<UUID> knowledgePointIds) {
        if (sortOrder != null) this.sortOrder = requireOrder(sortOrder);
        applyPayload(textContent, assetId, vocabularySenseIds, questionVersionIds, knowledgePointIds);
    }

    private void applyPayload(String text, UUID asset, List<UUID> senses, List<UUID> questions, List<UUID> points) {
        List<UUID> safeSenses = senses == null ? List.of() : List.copyOf(senses);
        List<UUID> safeQuestions = questions == null ? List.of() : List.copyOf(questions);
        List<UUID> safePoints = points == null ? List.of() : List.copyOf(points);
        boolean valid = switch (type) {
            case TEXT -> text != null && asset == null && safeSenses.isEmpty() && safeQuestions.isEmpty();
            case ASSET -> text == null && asset != null && safeSenses.isEmpty() && safeQuestions.isEmpty()
                    && safePoints.isEmpty();
            case VOCABULARY -> text == null && asset == null && !safeSenses.isEmpty() && safeQuestions.isEmpty()
                    && safePoints.isEmpty();
            case EXERCISE -> text == null && asset == null && !safeQuestions.isEmpty() && safeSenses.isEmpty()
                    && safePoints.isEmpty();
        };
        if (!valid) throw new LessonBlockInvalidException("Block fields do not match its block type");
        this.textContent = text;
        this.assetId = asset;
        this.vocabularySenseIds = safeSenses;
        this.questionVersionIds = safeQuestions;
        this.knowledgePointIds = safePoints;
    }

    private static int requireOrder(int order) {
        if (order < 0) throw new LessonBlockInvalidException("Block sortOrder must be non-negative");
        return order;
    }

    public UUID getId() { return id; }
    public UUID getLessonId() { return lessonId; }
    public BlockType getType() { return type; }
    public int getSortOrder() { return sortOrder; }
    public String getTextContent() { return textContent; }
    public UUID getAssetId() { return assetId; }
    public List<UUID> getVocabularySenseIds() { return vocabularySenseIds; }
    public List<UUID> getQuestionVersionIds() { return questionVersionIds; }
    public List<UUID> getKnowledgePointIds() { return knowledgePointIds; }
}
