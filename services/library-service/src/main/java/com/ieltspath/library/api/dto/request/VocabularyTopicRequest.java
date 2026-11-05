package com.ieltspath.library.api.dto.request;

import com.ieltspath.library.domain.catalog.VocabularyTopicDraft;
import com.ieltspath.library.domain.vo.ContentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VocabularyTopicRequest(
        @NotBlank @Size(max = 100) String slug,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 120) String nameVi,
        @Size(max = 500) String coverKey,
        @Min(0) Integer sortOrder,
        ContentStatus status
) {
    public VocabularyTopicDraft toDraft() {
        return new VocabularyTopicDraft(slug, name, nameVi, coverKey, sortOrder, status);
    }
}
