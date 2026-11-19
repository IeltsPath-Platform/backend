package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.api.dto.AccessLevel;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateQuestionRequest(
        @NotNull(message = "Cần chọn loại câu hỏi")
        QuestionType questionType,

        Skill skill,
        AccessLevel accessLevel,
        QuestionPurpose purpose,

        /** Optional key for spreadsheet imports and drafts. Unique across questions. */
        @Size(max = 100, message = "Mã tham chiếu (importRef) không được dài quá 100 ký tự")
        @Pattern(regexp = "^\\s*$|[A-Za-z0-9._-]+", message = "Mã tham chiếu (importRef) chỉ gồm chữ, số, dấu chấm, gạch dưới và gạch ngang")
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
