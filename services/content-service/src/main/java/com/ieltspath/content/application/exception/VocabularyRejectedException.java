package com.ieltspath.content.application.exception;

/** The vocabulary service refused a word as it was written; the message says why. */
public class VocabularyRejectedException extends RuntimeException {
    public VocabularyRejectedException(String message) {
        super(message);
    }
}
