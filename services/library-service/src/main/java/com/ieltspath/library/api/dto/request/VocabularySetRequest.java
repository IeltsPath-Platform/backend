package com.ieltspath.library.api.dto.request;

import com.ieltspath.library.domain.catalog.VocabularySetDraft;
import com.ieltspath.library.domain.vo.ContentStatus;
import com.ieltspath.library.domain.vo.VocabularySetKind;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VocabularySetRequest(
        @NotBlank @Size(max = 180) String title,
        @Size(max = 2000) String description,
        @NotNull VocabularySetKind kind,
        @Pattern(regexp = CatalogRequestValues.CEFR_OR_EMPTY, message = "cefrLevel must be A1, A2, B1, B2, C1 or C2")
        String cefrLevel,
        @Pattern(regexp = CatalogRequestValues.UUID_OR_EMPTY, message = "topicId must be a UUID") String topicId,
        @Size(max = 500) String coverKey,
        @Min(0) Integer sortOrder,
        ContentStatus status
) {
    public VocabularySetDraft toDraft() {
        return new VocabularySetDraft(title, description, kind, CatalogRequestValues.cefr(cefrLevel),
                CatalogRequestValues.uuid(topicId), coverKey, sortOrder, status);
    }
}
