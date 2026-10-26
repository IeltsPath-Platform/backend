package com.ieltspath.library.domain.catalog;

import com.ieltspath.library.domain.vo.CefrLevel;

import java.util.UUID;

/** Meaning-level dictionary data: level, topic and usage notes. Blank text clears a field. */
public record VocabularySenseDetails(
        CefrLevel cefrLevel,
        UUID topicId,
        String register,
        String grammarNote,
        String usageNote,
        String exampleTranslation
) {
    public VocabularySenseDetails {
        register = CatalogText.optional(register, 30, "register");
        grammarNote = CatalogText.optional(grammarNote, 2000, "grammarNote");
        usageNote = CatalogText.optional(usageNote, 2000, "usageNote");
        exampleTranslation = CatalogText.optional(exampleTranslation, 2000, "exampleTranslation");
    }
}
