package com.ieltspath.library.api.dto.request;

import com.ieltspath.library.domain.catalog.VocabularySenseDetails;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Replaces the meaning-level dictionary fields; an omitted or blank field is cleared. */
public record UpdateVocabularySenseRequest(
        @Pattern(regexp = CatalogRequestValues.CEFR_OR_EMPTY, message = "cefrLevel must be A1, A2, B1, B2, C1 or C2")
        String cefrLevel,
        @Pattern(regexp = CatalogRequestValues.UUID_OR_EMPTY, message = "topicId must be a UUID") String topicId,
        @Size(max = 30) String register,
        @Size(max = 2000) String grammarNote,
        @Size(max = 2000) String usageNote,
        @Size(max = 2000) String exampleTranslation
) {
    public VocabularySenseDetails toDetails() {
        return new VocabularySenseDetails(CatalogRequestValues.cefr(cefrLevel), CatalogRequestValues.uuid(topicId),
                register, grammarNote, usageNote, exampleTranslation);
    }
}
