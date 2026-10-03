package com.group01.learning.domain.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Chooses what a review at the theory stage shows: the lesson's TEXT blocks that teach the knowledge point (all TEXT
 * blocks when none is tagged) and up to {@link #MAX_QUICK_CHECK} of the lesson's auto-graded questions on it. The
 * choice is deterministic, so a quick check can be graded again from the same lesson without storing it first.
 */
public final class TheoryBlockSelector {
    public static final int MAX_QUICK_CHECK = 3;

    private static final Comparator<BlockView> BLOCK_ORDER = Comparator.comparingInt(BlockView::sortOrder)
            .thenComparing(block -> block.blockId().toString());
    private static final Comparator<QuestionView> QUESTION_ORDER = Comparator.comparingInt(QuestionView::sortOrder)
            .thenComparing(question -> question.questionVersionId().toString());

    public Selection select(List<BlockView> blocks, UUID knowledgePointId) {
        List<BlockView> ordered = blocks.stream().sorted(BLOCK_ORDER).toList();
        List<BlockView> texts = ordered.stream().filter(BlockView::text).toList();
        List<UUID> tagged = texts.stream().filter(block -> block.knowledgePointIds().contains(knowledgePointId))
                .map(BlockView::blockId).toList();
        boolean scoped = !tagged.isEmpty();
        List<UUID> theory = scoped ? tagged : texts.stream().map(BlockView::blockId).toList();
        List<UUID> quickCheck = ordered.stream().filter(BlockView::gradedExercise)
                .flatMap(block -> block.questions().stream().sorted(QUESTION_ORDER))
                .filter(question -> question.knowledgePointIds().contains(knowledgePointId))
                .limit(MAX_QUICK_CHECK).map(QuestionView::questionVersionId).toList();
        return new Selection(theory, scoped, quickCheck);
    }

    /**
     * {@code text} marks a TEXT block with content; {@code gradedExercise} an exercise block that is not an essay.
     */
    public record BlockView(UUID blockId, int sortOrder, boolean text, boolean gradedExercise,
                            List<UUID> knowledgePointIds, List<QuestionView> questions) {
        public BlockView {
            knowledgePointIds = knowledgePointIds == null ? List.of() : List.copyOf(knowledgePointIds);
            questions = questions == null ? List.of() : List.copyOf(questions);
        }
    }

    public record QuestionView(UUID questionVersionId, int sortOrder, List<UUID> knowledgePointIds) {
        public QuestionView {
            knowledgePointIds = knowledgePointIds == null ? List.of() : List.copyOf(knowledgePointIds);
        }
    }

    /** {@code knowledgePointScoped} is false when the lesson tags no TEXT block with the knowledge point. */
    public record Selection(List<UUID> theoryBlockIds, boolean knowledgePointScoped,
                            List<UUID> quickCheckQuestionIds) {}
}
