package com.group01.learning.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.group01.learning.application.result.LessonResult;
import com.group01.learning.domain.vo.LearningSkill;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record LessonResponse(UUID lessonId, UUID topicId, String code, String title, String summary,
                             int sortOrder, String status, List<Block> blocks, LearningSkill skill,
                             List<LearningSkill> skills) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Block(UUID blockId, String blockType, String blockKind, int sortOrder, String textContent,
                        Asset asset, List<UUID> vocabularySenseIds, Boolean passed, List<Question> questions,
                        List<Solution> solutions, EssayQuestion question,
                        Optional<LatestSubmission> latestSubmission, String sampleAnswer) {
        /** Essay blocks always carry {@code latestSubmission}, as {@code null} when the learner has none. */
        static Block from(LessonResult.Block result) {
            boolean essay = result.essay() != null;
            var latest = result.latestSubmission() == null ? null : new LatestSubmission(
                    result.latestSubmission().id(), result.latestSubmission().status(),
                    result.latestSubmission().overallBand(), result.latestSubmission().passed());
            return new Block(result.blockId(), result.blockType(), result.blockKind(), result.sortOrder(),
                    result.textContent(), result.asset() == null ? null : Asset.from(result.asset()),
                    result.vocabularySenseIds(), result.passed(),
                    result.questions() == null ? null : result.questions().stream().map(Question::from).toList(),
                    result.solutions() == null ? null : result.solutions().stream().map(solution -> new Solution(
                            solution.questionVersionId(), solution.correctAnswer(), solution.explanation())).toList(),
                    result.essay() == null ? null : EssayQuestion.from(result.essay()),
                    essay ? Optional.ofNullable(latest) : null, result.sampleAnswer());
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record LatestSubmission(UUID id, String status, BigDecimal overallBand, Boolean passed) {}

    /** A media asset has {@code mediaUrl}; {@code transcript} appears only once the lesson is completed. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Asset(UUID id, String assetType, String textContent, String mediaUrl, Integer durationSeconds,
                        String transcript) {
        static Asset from(LessonResult.Asset result) {
            return new Asset(result.id(), result.assetType(), result.textContent(), result.mediaUrl(),
                    result.durationSeconds(), result.transcript());
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Question(UUID questionVersionId, int sortOrder, String stem, List<Option> options, String hint) {
        public Question(UUID questionVersionId, int sortOrder, String stem, List<Option> options) {
            this(questionVersionId, sortOrder, stem, options, null);
        }

        static Question from(LessonResult.Question result) {
            return new Question(result.questionVersionId(), result.sortOrder(), result.stem(), result.options() == null ? null
                    : result.options().stream().map(option -> new Option(option.optionKey(), option.content(), option.sortOrder())).toList(),
                    result.hint());
        }
    }

    /** Essay prompt for the learner; never the answer spec, chart facts or model answer. */
    public record EssayQuestion(UUID questionVersionId, String stem, String task, Integer minWords,
                                BigDecimal passBand, List<Image> images) {
        static EssayQuestion from(LessonResult.EssayQuestion result) {
            return new EssayQuestion(result.questionVersionId(), result.stem(), result.task(), result.minWords(),
                    result.passBand(), result.images().stream()
                    .map(image -> new Image(image.mediaUrl(), image.altText())).toList());
        }
    }

    public record Image(String mediaUrl, String altText) {}
    public record Option(String optionKey, String content, int sortOrder) {}
    public record Solution(UUID questionVersionId, String correctAnswer, String explanation) {}

    public static LessonResponse from(LessonResult result) {
        return new LessonResponse(result.lessonId(), result.topicId(), result.code(), result.title(), result.summary(),
                result.sortOrder(), result.status(), result.blocks().stream().map(Block::from).toList(), result.skill(),
                result.skills().stream().sorted().toList());
    }
}
