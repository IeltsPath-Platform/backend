package com.ieltspath.content.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SearchPracticeTestsRequest(@NotNull String skill, @NotNull String scope,
                                         @Min(1) @Max(4) Integer partNo,
                                         @Size(max = 10) List<String> sources,
                                         @Size(max = 100) String query,
                                         @Size(max = 500) List<@Valid @NotNull Key> only,
                                         @Size(max = 500) List<@Valid @NotNull Key> exclude,
                                         @Min(0) Integer page, @Min(1) @Max(50) Integer size,
                                         @Size(max = 10) List<@NotNull @Pattern(regexp = "^[A-Z][A-Z_]{0,39}$") String> formats) {
    public record Key(@NotNull UUID packageId, @Min(1) @Max(4) Integer partNo) {}
}
