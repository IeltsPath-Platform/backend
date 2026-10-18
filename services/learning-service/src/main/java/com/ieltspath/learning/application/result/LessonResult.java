package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.vo.LearningSkill;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record LessonResult(UUID lessonId, UUID topicId, String code, String title, String summary,
                           int sortOrder, String status, List<Block> blocks, LearningSkill skill,
                           Set<LearningSkill> skills) {
    public LessonResult {
        skills = skills == null ? (skill == null ? Set.of() : Set.of(skill)) : skills;
    }
    public LessonResult(UUID lessonId, UUID topicId, String code, String title, String summary,
                        int sortOrder, String status, List<Block> blocks, LearningSkill skill) {
        this(lessonId, topicId, code, title, summary, sortOrder, status, blocks, skill, null);
    }
    public LessonResult(UUID lessonId, UUID topicId, String code, String title, String summary,
                        int sortOrder, String status, List<Block> blocks) {
        this(lessonId, topicId, code, title, summary, sortOrder, status, blocks, null);
    }
    /**
     * {@code blockKind} is set on exercise blocks; an essay block carries {@code essay} instead of questions, the
     * learner's {@code latestSubmission} (null when none) and {@code sampleAnswer} once the block has been passed.
     */
    public record Block(UUID blockId, String blockType, String blockKind, int sortOrder, String textContent,
                        Asset asset, List<UUID> vocabularySenseIds, Boolean passed,
                        List<Question> questions, List<Solution> solutions, EssayQuestion essay,
                        LatestSubmission latestSubmission, String sampleAnswer) {
        public Block(UUID blockId, String blockType, String blockKind, int sortOrder, String textContent,
                     Asset asset, List<UUID> vocabularySenseIds, Boolean passed,
                     List<Question> questions, List<Solution> solutions, EssayQuestion essay) {
            this(blockId, blockType, blockKind, sortOrder, textContent, asset, vocabularySenseIds, passed, questions,
                    solutions, essay, null, null);
        }
    }

    /** {@code overallBand} and {@code passed} are null unless the submission is GRADED (points debited). */
    public record LatestSubmission(UUID id, String status, BigDecimal overallBand, Boolean passed) {}

    /**
     * {@code textContent} is passage text; media assets carry {@code mediaUrl}. An audio transcript is set only once
     * the lesson is completed, because it gives the answers away.
     */
    public record Asset(UUID id, String assetType, String textContent, String mediaUrl, Integer durationSeconds,
                        String transcript) {}
    public record Question(UUID questionVersionId, int sortOrder, String stem, List<Option> options, String hint) {
        public Question(UUID questionVersionId, int sortOrder, String stem, List<Option> options) {
            this(questionVersionId, sortOrder, stem, options, null);
        }
    }
    public record Option(String optionKey, String content, int sortOrder) {}
    public record Solution(UUID questionVersionId, String correctAnswer, String explanation) {}
    public record EssayQuestion(UUID questionVersionId, String stem, String task, Integer minWords,
                                BigDecimal passBand, List<Image> images) {}
    public record Image(String mediaUrl, String altText) {}
}
