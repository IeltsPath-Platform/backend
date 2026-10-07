package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.api.dto.AccessLevel;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;
import jakarta.validation.constraints.NotNull;

public record CreateQuestionRequest(
        @NotNull(message = "questionType is required")
        QuestionType questionType,

        Skill skill,
        AccessLevel accessLevel,
        QuestionPurpose purpose
) {
    public CreateQuestionRequest {
        purpose = purpose != null ? purpose : QuestionPurpose.LEARNING;
    }

    public CreateQuestionRequest(QuestionType questionType, Skill skill, AccessLevel accessLevel) {
        this(questionType, skill, accessLevel, QuestionPurpose.LEARNING);
    }
}
