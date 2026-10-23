package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.api.dto.AccessLevel;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateQuestionRequest(
        @NotNull(message = "questionType is required")
        QuestionType questionType,

        Skill skill,
        AccessLevel accessLevel,
        QuestionPurpose purpose,

        /** Optional key for spreadsheet imports and drafts. Unique across questions. */
        @Size(max = 100, message = "importRef must not exceed 100 characters")
        @Pattern(regexp = "^\\s*$|[A-Za-z0-9._-]+", message = "importRef may use letters, digits, dot, underscore and dash")
        String importRef
) {
    public CreateQuestionRequest {
        purpose = purpose != null ? purpose : QuestionPurpose.LEARNING;
    }

    public CreateQuestionRequest(QuestionType questionType, Skill skill, AccessLevel accessLevel,
                                 QuestionPurpose purpose) {
        this(questionType, skill, accessLevel, purpose, null);
    }

    public CreateQuestionRequest(QuestionType questionType, Skill skill, AccessLevel accessLevel) {
        this(questionType, skill, accessLevel, QuestionPurpose.LEARNING, null);
    }
}
