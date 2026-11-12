package com.ieltspath.learning.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record OpenPracticeSessionRequest(@NotNull UUID requestId,
        @NotNull @Pattern(regexp = "WRITING|SPEAKING|VOCABULARY") String kind) {}
