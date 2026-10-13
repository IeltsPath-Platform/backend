package com.ieltspath.content.domain.exception;

import com.ieltspath.content.domain.vo.LessonResourceType;

public class DuplicateLessonResourceOrderException extends ContentDomainException {
    public DuplicateLessonResourceOrderException(LessonResourceType type, int sortOrder) {
        super("The lesson already has a " + type + " resource at sortOrder " + sortOrder);
    }
}
