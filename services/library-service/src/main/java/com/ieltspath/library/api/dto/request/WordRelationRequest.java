package com.ieltspath.library.api.dto.request;

import com.ieltspath.library.domain.catalog.WordRelation;
import com.ieltspath.library.domain.vo.RelationKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WordRelationRequest(
        @NotBlank @Size(max = 255) String relatedLemma,
        @Pattern(regexp = CatalogRequestValues.UUID_OR_EMPTY, message = "relatedItemId must be a UUID") String relatedItemId,
        @NotNull RelationKind kind
) {
    public WordRelation toRelation() {
        return new WordRelation(relatedLemma, CatalogRequestValues.uuid(relatedItemId), kind);
    }
}
