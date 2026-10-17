package com.ieltspath.content.api.dto.internal;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record QuestionDifficultiesRequest(
        @NotEmpty @Size(max = 500) List<@NotNull UUID> questionVersionIds
) {}
