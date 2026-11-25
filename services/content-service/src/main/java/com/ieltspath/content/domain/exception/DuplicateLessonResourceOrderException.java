package com.ieltspath.content.domain.exception;

import com.ieltspath.content.domain.vo.LessonResourceType;

public class DuplicateLessonResourceOrderException extends ContentDomainException {
    public DuplicateLessonResourceOrderException(LessonResourceType type, int sortOrder) {
        super("Bài học đã có tài nguyên loại " + type + " ở thứ tự " + sortOrder);
    }
}
