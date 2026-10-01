package com.group01.learning.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.group01.learning.application.result.LessonResult;

import java.util.List;
import java.util.UUID;

public record LessonResponse(UUID lessonId, UUID topicId, String code, String title, String summary,
                             int sortOrder, String status, List<Block> blocks) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Block(UUID blockId, String blockType, int sortOrder, String textContent, Asset asset,
                        List<UUID> vocabularySenseIds, Boolean passed, List<Question> questions,
                        List<Solution> solutions) {
        static Block from(LessonResult.Block result) {
            return new Block(result.blockId(), result.blockType(), result.sortOrder(), result.textContent(),
                    result.asset() == null ? null : Asset.from(result.asset()), result.vocabularySenseIds(), result.passed(),
                    result.questions() == null ? null : result.questions().stream().map(Question::from).toList(),
                    result.solutions() == null ? null : result.solutions().stream().map(solution -> new Solution(
                            solution.questionVersionId(), solution.correctAnswer(), solution.explanation())).toList());
        }
    }
    public record Asset(UUID id, String assetType, String textContent, String mediaReference, Integer durationSeconds) {
        static Asset from(LessonResult.Asset result) {
            return new Asset(result.id(), result.assetType(), result.textContent(), result.mediaReference(), result.durationSeconds());
        }
    }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Question(UUID questionVersionId, int sortOrder, String stem, List<Option> options) {
        static Question from(LessonResult.Question result) {
            return new Question(result.questionVersionId(), result.sortOrder(), result.stem(), result.options() == null ? null
                    : result.options().stream().map(option -> new Option(option.optionKey(), option.content(), option.sortOrder())).toList());
        }
    }
    public record Option(String optionKey, String content, int sortOrder) {}
    public record Solution(UUID questionVersionId, String correctAnswer, String explanation) {}

    public static LessonResponse from(LessonResult result) {
        return new LessonResponse(result.lessonId(), result.topicId(), result.code(), result.title(), result.summary(),
                result.sortOrder(), result.status(), result.blocks().stream().map(Block::from).toList());
    }
}
