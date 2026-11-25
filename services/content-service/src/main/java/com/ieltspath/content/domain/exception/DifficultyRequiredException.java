package com.ieltspath.content.domain.exception;

public class DifficultyRequiredException extends ContentDomainException {
    public DifficultyRequiredException() {
        super("Cần chọn độ khó cho câu hỏi");
    }
}
