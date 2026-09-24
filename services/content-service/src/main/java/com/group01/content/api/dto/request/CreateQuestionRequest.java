package com.group01.content.api.dto.request;

import com.group01.content.api.dto.AccessLevel;
import com.group01.content.domain.vo.QuestionType;
import com.group01.content.domain.vo.Skill;
import jakarta.validation.constraints.NotNull;

public record CreateQuestionRequest(
        @NotNull(message = "questionType is required")
        QuestionType questionType,

        Skill skill,
        AccessLevel accessLevel
) {}
