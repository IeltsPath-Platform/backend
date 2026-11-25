package com.ieltspath.content.domain.exception;

import java.util.UUID;

/** A course that still holds topics, tests or practice-test checkpoints is stopped, never deleted. */
public class CourseNotEmptyException extends ContentDomainException {
    public CourseNotEmptyException(UUID id) {
        super("Khóa học còn chủ đề, đề cuối khóa hoặc bài kiểm tra nên không xóa được. Hãy chuyển sang \"Ngừng dùng\" để học viên không còn thấy.");
    }
}
