package com.group01.content.domain.exception;

import com.group01.content.domain.vo.QuestionUsageConflict;

import java.util.List;
import java.util.stream.Collectors;

/** A question cannot be published for more than one lesson or learning/test package. */
public class QuestionAlreadyUsedException extends ContentDomainException {
    public QuestionAlreadyUsedException(List<QuestionUsageConflict> conflicts) {
        super("Questions already used: " + conflicts.stream().limit(10)
                .map(conflict -> conflict.questionId() + " (" + conflict.ownerType() + " " + conflict.ownerCode() + ")")
                .collect(Collectors.joining(", ")));
    }
}
