package com.ieltspath.library.application.query;

import java.util.Locale;

public record VocabularySuggestionsQuery(String text, int page, int size) {
    public VocabularySuggestionsQuery {
        if (text == null || text.length() > 100 || page < 0 || size < 1 || size > 50) {
            throw new IllegalArgumentException("Invalid vocabulary suggestion query");
        }
        text = text.trim().toLowerCase(Locale.ROOT);
    }

    public int length() {
        return text.codePointCount(0, text.length());
    }

    public long offset() {
        return (long) page * size;
    }
}
