package com.ieltspath.library.domain.catalog;

/** A phrase the word commonly appears in. */
public record Collocation(String phrase, String meaningVi, String example) {
    public Collocation {
        phrase = CatalogText.required(phrase, 255, "phrase");
        meaningVi = CatalogText.optional(meaningVi, 1000, "meaningVi");
        example = CatalogText.optional(example, 2000, "example");
    }
}
