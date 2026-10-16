package com.ieltspath.assessment.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Optional body of a submit: the seconds the learner spent in each section. */
public record SubmitAttemptRequest(@Size(max = 20) List<@Valid SectionSeconds> sectionSeconds) {
    public record SectionSeconds(@NotNull UUID sectionId, @Min(0) @Max(86400) int seconds) {}
}
