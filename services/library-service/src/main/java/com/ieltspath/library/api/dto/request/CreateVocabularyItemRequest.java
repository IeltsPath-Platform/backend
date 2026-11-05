package com.ieltspath.library.api.dto.request;

import com.ieltspath.library.domain.catalog.VocabularyItemDetails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A new word. The US/UK IPA and audio are optional and are stored with the word itself, in the same transaction, so the
 * word is never saved without the pronunciation the author entered.
 */
public record CreateVocabularyItemRequest(
        @NotBlank(message = "lemma is required")
        @Size(max = 255, message = "lemma must not exceed 255 characters")
        String lemma,

        String ipa,
        String pronunciationAudioReference,

        @Size(max = 255, message = "ipaUs must not exceed 255 characters") String ipaUs,
        @Size(max = 255, message = "ipaUk must not exceed 255 characters") String ipaUk,
        @Size(max = 500, message = "audioUsReference must not exceed 500 characters") String audioUsReference,
        @Size(max = 500, message = "audioUkReference must not exceed 500 characters") String audioUkReference
) {
    /** The dictionary fields of the word. A new word has no frequency rank, academic-list flag or word family yet. */
    public VocabularyItemDetails toDictionary() {
        return new VocabularyItemDetails(ipaUk, ipaUs, audioUkReference, audioUsReference, null, false, null);
    }
}
