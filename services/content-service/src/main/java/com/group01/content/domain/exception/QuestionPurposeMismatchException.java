package com.group01.content.domain.exception;

import com.group01.content.domain.vo.QuestionPurpose;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** A package can only publish questions intended for its learning or exam purpose. */
public class QuestionPurposeMismatchException extends ContentDomainException {
    public QuestionPurposeMismatchException(QuestionPurpose requiredPurpose, List<UUID> questionIds) {
        super("Questions must have purpose " + requiredPurpose + ": " + questionIds.stream().limit(10)
                .map(UUID::toString).collect(Collectors.joining(", ")));
    }
}
