package com.ieltspath.learning.domain.exception;

public class PracticeAttemptAlreadySubmittedException extends RuntimeException {
    public PracticeAttemptAlreadySubmittedException() {
        super("Lượt luyện tập này đã được nộp trước đó với một yêu cầu khác.");
    }
}
