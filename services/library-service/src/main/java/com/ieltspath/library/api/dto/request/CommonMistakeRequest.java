package com.ieltspath.library.api.dto.request;

import com.ieltspath.library.domain.catalog.CommonMistake;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommonMistakeRequest(
        @NotBlank @Size(max = 2000) String wrongUsage,
        @NotBlank @Size(max = 2000) String correctUsage,
        @Size(max = 2000) String explanation
) {
    public CommonMistake toMistake() {
        return new CommonMistake(wrongUsage, correctUsage, explanation);
    }
}
