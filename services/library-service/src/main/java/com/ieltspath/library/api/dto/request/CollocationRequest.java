package com.ieltspath.library.api.dto.request;

import com.ieltspath.library.domain.catalog.Collocation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CollocationRequest(
        @NotBlank @Size(max = 255) String phrase,
        @Size(max = 1000) String meaningVi,
        @Size(max = 2000) String example
) {
    public Collocation toCollocation() {
        return new Collocation(phrase, meaningVi, example);
    }
}
