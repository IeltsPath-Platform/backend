package com.ieltspath.content.domain.vo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LessonAuthoringSnapshot(UUID id, UUID topicId, String code, String title, String summary,
        String summaryMarkdown, Integer estimatedMinutes, int sortOrder, LessonStatus status,
        Instant createdAt, Instant updatedAt, List<UUID> knowledgePointIds, List<Block> blocks) {
    public LessonAuthoringSnapshot {
        knowledgePointIds = knowledgePointIds == null ? List.of() : List.copyOf(knowledgePointIds);
        blocks = blocks == null ? List.of() : List.copyOf(blocks);
    }
    public record Block(UUID id, int sortOrder, BlockType blockType, String textContent, UUID assetId,
                        List<UUID> vocabularySenseIds, List<UUID> knowledgePointIds, List<QuestionVersion> questions) {
        public Block {
            vocabularySenseIds = vocabularySenseIds == null ? List.of() : List.copyOf(vocabularySenseIds);
            knowledgePointIds = knowledgePointIds == null ? List.of() : List.copyOf(knowledgePointIds);
            questions = questions == null ? List.of() : List.copyOf(questions);
        }
    }
    public record QuestionVersion(UUID id, int versionNumber, QuestionType questionType, Skill skill,
                                  QuestionPurpose purpose, PublicationStatus status, String stem,
                                  String answerSpecJson) {}
}
