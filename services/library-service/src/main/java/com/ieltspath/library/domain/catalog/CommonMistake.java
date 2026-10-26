package com.ieltspath.library.domain.catalog;

/** A typical wrong use of the word and its correction. */
public record CommonMistake(String wrongUsage, String correctUsage, String explanation) {
    public CommonMistake {
        wrongUsage = CatalogText.required(wrongUsage, 2000, "wrongUsage");
        correctUsage = CatalogText.required(correctUsage, 2000, "correctUsage");
        explanation = CatalogText.optional(explanation, 2000, "explanation");
    }
}
