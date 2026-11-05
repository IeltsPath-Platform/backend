package com.ieltspath.library.domain.catalog;

import com.ieltspath.library.domain.exception.InvalidDataException;

/**
 * Word-level dictionary data an author maintains. Blank text clears a field; the frequency rank stays empty until
 * a licensed frequency source supplies it.
 */
public record VocabularyItemDetails(
        String ipaUk,
        String ipaUs,
        String audioUkReference,
        String audioUsReference,
        Integer frequencyRank,
        boolean academicWordList,
        String wordFamily
) {
    public VocabularyItemDetails {
        ipaUk = CatalogText.optional(ipaUk, 255, "ipaUk");
        ipaUs = CatalogText.optional(ipaUs, 255, "ipaUs");
        audioUkReference = CatalogText.optional(audioUkReference, 500, "audioUkReference");
        audioUsReference = CatalogText.optional(audioUsReference, 500, "audioUsReference");
        if (frequencyRank != null && frequencyRank < 1) {
            throw new InvalidDataException("frequencyRank must be at least 1");
        }
        wordFamily = CatalogText.optional(wordFamily, 2000, "wordFamily");
    }
}
