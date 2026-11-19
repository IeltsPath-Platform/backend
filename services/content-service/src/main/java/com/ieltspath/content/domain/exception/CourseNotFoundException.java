package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class CourseNotFoundException extends ContentDomainException {
    public CourseNotFoundException(UUID id) {
        super("Không tìm thấy khóa học");
    }
}
