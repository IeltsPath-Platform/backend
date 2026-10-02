package com.group01.learning.application.service;

import com.group01.learning.application.port.LearningContentClient.Asset;
import com.group01.learning.application.port.LearningContentClient.Block;
import com.group01.learning.application.port.LearningContentClient.Lesson;
import com.group01.learning.application.port.LearningContentClient.Question;
import com.group01.learning.application.port.LearningContentClient.QuestionAsset;
import com.group01.learning.application.result.LessonResult;
import com.group01.learning.domain.repository.WritingSubmissionRepository.BlockSummary;
import com.group01.learning.domain.service.AnswerSpecGrader;
import com.group01.learning.domain.service.LessonHintPolicy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * Builds the learner's view of a lesson. Answers, explanations and transcripts appear only once the learner has earned
 * them: solutions after the block passed, a transcript after the lesson completed, a sample essay after the essay block
 * passed; answer specs and chart facts never.
 */
@Component
public class LessonViewAssembler {
    private final AnswerSpecGrader grader = new AnswerSpecGrader();
    private final LessonHintPolicy hints = new LessonHintPolicy();

    public List<LessonResult.Block> blocks(Lesson lesson, Set<UUID> passedBlocks, boolean lessonCompleted,
                                           Map<UUID, BlockSummary> essays, Map<UUID, Set<UUID>> openedByBlock) {
        return lesson.blocks().stream()
                .sorted(Comparator.comparingInt(Block::sortOrder).thenComparing(block -> block.blockId().toString()))
                .map(block -> block(block, passedBlocks.contains(block.blockId()), lessonCompleted,
                        essays.get(block.blockId()), openedByBlock.getOrDefault(block.blockId(), Set.of())))
                .toList();
    }

    /** A question's hint, shown after a wrong answer until the block passes, for eligible question types. */
    public String hint(Question question, boolean blockPassed, Set<UUID> opened) {
        if (blockPassed || !opened.contains(question.questionVersionId())) return null;
        int optionCount = question.options() == null ? 0 : question.options().size();
        return hints.eligible(question.answerSpec(), optionCount) ? question.hint() : null;
    }

    public static List<Question> orderedQuestions(Block block) {
        return block.questions().stream().sorted(Comparator.comparingInt(Question::sortOrder)
                .thenComparing(question -> question.questionVersionId().toString())).toList();
    }

    private LessonResult.Block block(Block block, boolean passed, boolean lessonCompleted, BlockSummary essay,
                                     Set<UUID> opened) {
        var asset = block.asset() == null ? null : learnerAsset(block.asset(), lessonCompleted);
        if (block.isEssay()) {
            // The model answer stays hidden until the learner has passed the block once.
            boolean essayPassed = essay != null && essay.passed();
            var latest = essay == null ? null : new LessonResult.LatestSubmission(essay.latestId(),
                    essay.latestStatus().name(), essay.latestOverallBand(), essay.latestPassed());
            return new LessonResult.Block(block.blockId(), block.blockType(), "ESSAY", block.sortOrder(), null, null,
                    null, null, null, null, essayQuestion(block), latest,
                    essayPassed ? block.questions().getFirst().explanation() : null);
        }
        boolean exercise = block.isExercise();
        var questions = exercise ? orderedQuestions(block).stream().map(question -> new LessonResult.Question(
                question.questionVersionId(), question.sortOrder(), question.stem(), question.options() == null ? null
                : question.options().stream().map(option -> new LessonResult.Option(option.optionKey(), option.content(),
                        option.sortOrder())).toList(), hint(question, passed, opened))).toList() : null;
        var solutions = exercise && passed ? orderedQuestions(block).stream().map(question -> new LessonResult.Solution(
                question.questionVersionId(), grader.grade(question.answerSpec(), null).correctAnswer(),
                question.explanation())).toList() : null;
        return new LessonResult.Block(block.blockId(), block.blockType(), exercise ? "EXERCISE" : null,
                block.sortOrder(), block.textContent(), asset, block.vocabularySenseIds(), exercise ? passed : null,
                questions, solutions, null);
    }

    /** Passage text is shown as is; an audio transcript gives the answers away, so it waits for completion. */
    private static LessonResult.Asset learnerAsset(Asset asset, boolean lessonCompleted) {
        if ("PASSAGE".equals(asset.assetType())) {
            return new LessonResult.Asset(asset.id(), asset.assetType(), asset.textContent(), null, null, null);
        }
        String transcript = "AUDIO".equals(asset.assetType()) && lessonCompleted ? asset.textContent() : null;
        return new LessonResult.Asset(asset.id(), asset.assetType(), null, asset.mediaUrl(), asset.durationSeconds(),
                transcript);
    }

    /** The learner sees the prompt and images only; never the answer spec, chart facts or model answer. */
    private static LessonResult.EssayQuestion essayQuestion(Block block) {
        Question question = block.questions().getFirst();
        Map<String, Object> spec = question.answerSpec() == null ? Map.of() : question.answerSpec();
        List<LessonResult.Image> images = question.assets() == null ? List.of() : question.assets().stream()
                .filter(asset -> "IMAGE".equals(asset.assetType()))
                .sorted(Comparator.comparingInt(QuestionAsset::sortOrder))
                .map(asset -> new LessonResult.Image(asset.mediaUrl(), asset.altText())).toList();
        return new LessonResult.EssayQuestion(question.questionVersionId(), question.stem(),
                spec.get("task") instanceof String task ? task : null,
                spec.get("minWords") instanceof Number words ? words.intValue() : null,
                spec.get("passBand") instanceof Number band ? new BigDecimal(band.toString()) : null,
                images);
    }
}
