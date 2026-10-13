package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient.Lesson;

import java.util.List;

/** The theory of a review: the Markdown summary of the lesson the review points at. */
public final class TheoryFocus {
    public static final String LESSON_SCOPE = "LESSON";

    public Focus of(Lesson lesson) {
        String summary = lesson.summaryMarkdown();
        return new Focus(summary == null || summary.isBlank() ? List.of() : List.of(summary), LESSON_SCOPE);
    }

    public record Focus(List<String> theory, String scope) {}
}
