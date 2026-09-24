package com.group01.content.application.command;

import com.group01.content.domain.vo.QuestionType;
import com.group01.content.domain.vo.Skill;

public record CreateQuestionCommand(
        QuestionType questionType,
        Skill skill,
        String requiredFeatureKey
) {}
