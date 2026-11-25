package com.ieltspath.content.domain.exception;

import java.util.UUID;

/** A question version may appear once in a package version, in any section of it. */
public class QuestionAlreadyInVersionException extends ContentDomainException {
    public QuestionAlreadyInVersionException(UUID questionVersionId) {
        super("Câu hỏi đã có trong phiên bản gói này");
    }
}
