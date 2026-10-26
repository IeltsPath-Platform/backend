package com.ieltspath.library.application.result;

/** Entry counts shown on the dictionary tabs. */
public record CatalogCountsResult(int definitions, int collocations, int thesaurus, int commonMistakes, int examples) {
}
