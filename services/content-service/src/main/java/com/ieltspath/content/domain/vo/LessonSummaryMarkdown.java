package com.ieltspath.content.domain.vo;

/** The Markdown summary of a lesson. Blank text means the lesson has no summary. */
public final class LessonSummaryMarkdown {
    public static final int MAX_LENGTH = 20_000;

    private LessonSummaryMarkdown() {
    }

    /** Returns the text to store, or {@code null} when the summary is blank. */
    public static String normalize(String markdown) {
        if (markdown == null || markdown.isBlank()) return null;
        if (markdown.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("summaryMarkdown exceeds " + MAX_LENGTH + " characters");
        }
        return markdown.strip();
    }
}
