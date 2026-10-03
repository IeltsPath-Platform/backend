package com.group01.learning.application.service;

import com.group01.learning.application.port.LearningContentClient.Block;
import com.group01.learning.application.port.LearningContentClient.Lesson;
import com.group01.learning.application.port.LearningContentClient.Question;
import com.group01.learning.domain.service.TheoryBlockSelector;
import com.group01.learning.domain.service.TheoryBlockSelector.BlockView;
import com.group01.learning.domain.service.TheoryBlockSelector.QuestionView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The theory and quick check of one knowledge point in the lesson a review points at ({@link TheoryBlockSelector}). */
public final class TheoryFocus {
    private final TheoryBlockSelector selector = new TheoryBlockSelector();

    public Focus of(Lesson lesson, UUID knowledgePointId) {
        Map<UUID, Block> blocks = new HashMap<>();
        Map<UUID, Question> questions = new HashMap<>();
        List<BlockView> views = lesson.blocks().stream().map(block -> {
            blocks.put(block.blockId(), block);
            List<Question> blockQuestions = block.questions() == null ? List.of() : block.questions();
            blockQuestions.forEach(question -> questions.put(question.questionVersionId(), question));
            return new BlockView(block.blockId(), block.sortOrder(),
                    "TEXT".equals(block.blockType()) && block.textContent() != null, block.isExercise(),
                    block.knowledgePointIds(), blockQuestions.stream().map(question -> new QuestionView(
                            question.questionVersionId(), question.sortOrder(), question.knowledgePointIds())).toList());
        }).toList();
        var selection = selector.select(views, knowledgePointId);
        return new Focus(selection.theoryBlockIds().stream().map(id -> blocks.get(id).textContent()).toList(),
                selection.knowledgePointScoped() ? "KNOWLEDGE_POINT" : "LESSON_FALLBACK",
                selection.quickCheckQuestionIds().stream().map(questions::get).toList());
    }

    /** {@code quickCheck} carries answer specs for grading; responses expose only stem, options and hint. */
    public record Focus(List<String> theory, String scope, List<Question> quickCheck) {}
}
