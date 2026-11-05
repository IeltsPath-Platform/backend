package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionType;

import java.util.List;
import java.util.UUID;

/**
 * What an author asks the AI for to fill a lesson that has no practice set: the knowledge points the lesson teaches, the
 * lesson's title for context, the words the lesson teaches (the passage should use them), a question type, a count and a
 * difficulty. A request stored before the words existed has none.
 */
public record PracticeSetDraftRequest(
        List<UUID> knowledgePointIds,
        String lessonTitle,
        List<String> vocabulary,
        QuestionType questionType,
        int count,
        QuestionDifficulty difficulty,
        String instructions
) {
    public PracticeSetDraftRequest {
        vocabulary = vocabulary == null ? List.of() : List.copyOf(vocabulary);
    }
}
