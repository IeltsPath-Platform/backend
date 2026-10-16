package com.ieltspath.learning.domain.vo;

/** Where a question event came from. {@link #value()} is the text stored in {@code question_attempt_events.source}. */
public enum QuestionEventSource {
    LESSON_EXERCISE("lesson_exercise"),
    PRACTICE_SET("practice_set"),
    REVIEW_SET("review_set"),
    LESSON_WRITING("lesson_writing"),
    ASSESSMENT("assessment"),
    DIAGNOSTIC_PROBE("diagnostic_probe");

    private final String value;

    QuestionEventSource(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static QuestionEventSource fromValue(String stored) {
        for (QuestionEventSource source : values()) {
            if (source.value.equals(stored)) return source;
        }
        throw new IllegalArgumentException("Unknown question event source: " + stored);
    }
}
