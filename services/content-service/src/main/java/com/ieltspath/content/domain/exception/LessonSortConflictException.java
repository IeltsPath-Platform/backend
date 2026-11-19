package com.ieltspath.content.domain.exception;

public class LessonSortConflictException extends ContentDomainException {
    public LessonSortConflictException() {
        super("Thứ tự gửi lên phải gồm mỗi mục hiện có đúng một lần");
    }

    public LessonSortConflictException(String message) {
        super(message);
    }
}
