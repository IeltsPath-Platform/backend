package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class LessonResourceNotFoundException extends ContentDomainException {
    public LessonResourceNotFoundException(UUID lessonId, UUID resourceId) {
        super("Không tìm thấy tài nguyên của bài học");
    }
}
