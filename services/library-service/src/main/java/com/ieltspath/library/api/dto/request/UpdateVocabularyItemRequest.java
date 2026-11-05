package com.ieltspath.library.api.dto.request;

import com.ieltspath.library.domain.catalog.VocabularyItemDetails;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Replaces the word-level dictionary fields; an omitted or blank field is cleared. */
public record UpdateVocabularyItemRequest(
        @Size(max = 255) String ipaUk,
        @Size(max = 255) String ipaUs,
        @Size(max = 500) String audioUkReference,
        @Size(max = 500) String audioUsReference,
        @Min(1) Integer frequencyRank,
        Boolean academicWordList,
        @Size(max = 2000) String wordFamily
) {
    public VocabularyItemDetails toDetails() {
        return new VocabularyItemDetails(ipaUk, ipaUs, audioUkReference, audioUsReference, frequencyRank,
                Boolean.TRUE.equals(academicWordList), wordFamily);
    }
}
