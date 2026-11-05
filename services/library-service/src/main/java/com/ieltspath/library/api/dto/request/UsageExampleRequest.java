package com.ieltspath.library.api.dto.request;

import com.ieltspath.library.domain.catalog.UsageExample;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsageExampleRequest(
        @NotBlank @Size(max = 2000) String sentence,
        @NotBlank @Size(max = 2000) String translationVi
) {
    public UsageExample toExample() {
        return new UsageExample(sentence, translationVi);
    }
}
