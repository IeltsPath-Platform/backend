package com.group01.learning.application.result;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record LessonResult(UUID lessonId, UUID topicId, String code, String title, String summary,
                           int sortOrder, String status, List<Block> blocks) {
    /** {@code blockKind} is set on exercise blocks; an essay block carries {@code essay} instead of questions. */
    public record Block(UUID blockId, String blockType, String blockKind, int sortOrder, String textContent,
                        Asset asset, List<UUID> vocabularySenseIds, Boolean passed,
                        List<Question> questions, List<Solution> solutions, EssayQuestion essay) {}

    /**
     * {@code textContent} is passage text; media assets carry {@code mediaUrl}. An audio transcript is set only once
     * the lesson is completed, because it gives the answers away.
     */
    public record Asset(UUID id, String assetType, String textContent, String mediaUrl, Integer durationSeconds,
                        String transcript) {}
    public record Question(UUID questionVersionId, int sortOrder, String stem, List<Option> options) {}
    public record Option(String optionKey, String content, int sortOrder) {}
    public record Solution(UUID questionVersionId, String correctAnswer, String explanation) {}
    public record EssayQuestion(UUID questionVersionId, String stem, String task, Integer minWords,
                                BigDecimal passBand, List<Image> images) {}
    public record Image(String mediaUrl, String altText) {}
}
