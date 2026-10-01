package com.group01.learning.application.result;

import java.util.List;
import java.util.UUID;

public record LessonResult(UUID lessonId, UUID topicId, String code, String title, String summary,
                           int sortOrder, String status, List<Block> blocks) {
    public record Block(UUID blockId, String blockType, int sortOrder, String textContent,
                        Asset asset, List<UUID> vocabularySenseIds, Boolean passed,
                        List<Question> questions, List<Solution> solutions) {}
    public record Asset(UUID id, String assetType, String textContent, String mediaReference,
                        Integer durationSeconds) {}
    public record Question(UUID questionVersionId, int sortOrder, String stem, List<Option> options) {}
    public record Option(String optionKey, String content, int sortOrder) {}
    public record Solution(UUID questionVersionId, String correctAnswer, String explanation) {}
}
