package com.ieltspath.content.domain.exception;

public class QuestionDifficultyRequiredForPublicationException extends ContentDomainException {
    public QuestionDifficultyRequiredForPublicationException() {
        super("Cần chọn độ khó của câu hỏi trước khi xuất bản");
    }
}
