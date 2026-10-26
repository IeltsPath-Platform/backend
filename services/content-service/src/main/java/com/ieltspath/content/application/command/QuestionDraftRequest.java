package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionType;

import java.util.UUID;

/** What an author asks the AI for: one knowledge point, a question type, a count and a difficulty. */
public record QuestionDraftRequest(
        UUID knowledgePointId,
        QuestionType questionType,
        int count,
        QuestionDifficulty difficulty,
        String instructions
) {
}
