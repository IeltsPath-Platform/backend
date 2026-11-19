package com.ieltspath.content.domain.exception;

public class LessonExerciseLockedException extends ContentDomainException {
    public LessonExerciseLockedException() {
        super("Chỉ sửa được khối bài tập khi bài học còn là bản nháp");
    }
}
