package com.ieltspath.library.domain.catalog;

/** An example sentence with its Vietnamese translation. */
public record UsageExample(String sentence, String translationVi) {
    public UsageExample {
        sentence = CatalogText.required(sentence, 2000, "sentence");
        translationVi = CatalogText.required(translationVi, 2000, "translationVi");
    }
}
