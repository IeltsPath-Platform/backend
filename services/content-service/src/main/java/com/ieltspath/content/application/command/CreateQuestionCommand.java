package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;

public record CreateQuestionCommand(
        QuestionType questionType,
        Skill skill,
        String requiredFeatureKey,
        QuestionPurpose purpose
) {}
