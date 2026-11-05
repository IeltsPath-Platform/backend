package com.ieltspath.library.domain.catalog;

import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.vo.RelationKind;

import java.util.UUID;

/** A thesaurus entry; the related word links to its own dictionary page when it exists in the catalog. */
public record WordRelation(String relatedLemma, UUID relatedItemId, RelationKind kind) {
    public WordRelation {
        relatedLemma = CatalogText.required(relatedLemma, 255, "relatedLemma");
        if (kind == null) {
            throw new InvalidDataException("kind is required");
        }
    }
}
