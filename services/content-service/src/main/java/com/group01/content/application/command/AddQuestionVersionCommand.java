package com.group01.content.application.command;

import com.group01.content.domain.vo.QuestionDifficulty;
import com.group01.content.domain.vo.QuestionOptionPayload;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AddQuestionVersionCommand(
        UUID questionId,
        int versionNumber,
        String stem,
        List<QuestionOptionPayload> options,
        String answerSpecJson,
        String explanation,
        QuestionDifficulty difficulty,
        List<KnowledgePointInput> knowledgePoints
) {
    public record KnowledgePointInput(
            UUID questionVersionId,
            UUID knowledgePointId,
            BigDecimal weight
    ) {
    }
}
