package com.ieltspath.content.domain.exception;

import com.ieltspath.content.domain.vo.QuestionUsageConflict;

import java.util.List;
import java.util.stream.Collectors;

/** A question cannot be published for more than one lesson or learning/test package. */
public class QuestionAlreadyUsedException extends ContentDomainException {
    public QuestionAlreadyUsedException(List<QuestionUsageConflict> conflicts) {
        super("Có câu hỏi đã được dùng ở bài học hoặc gói khác: " + conflicts.stream().limit(10)
                .map(conflict -> conflict.questionId() + " (" + conflict.ownerType() + " " + conflict.ownerCode() + ")")
                .collect(Collectors.joining(", ")));
    }
}
