package com.ieltspath.learning.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** {@code essayText} is checked by the use case so an empty or wrong-length essay gets its own error code. */
public record SubmitEssayRequest(@NotNull UUID requestId, String essayText) {}
