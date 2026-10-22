package com.ieltspath.content.application.exception;

public class VocabularyServiceUnavailableException extends RuntimeException {
    public VocabularyServiceUnavailableException(Throwable cause) {
        super("Vocabulary service is unavailable", cause);
    }
}
