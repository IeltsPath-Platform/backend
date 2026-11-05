package com.ieltspath.learning.domain.exception;

public class LearningGoalNotFoundException extends RuntimeException {
    public LearningGoalNotFoundException(String message) {
        super(message);
    }
}

