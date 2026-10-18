package com.ieltspath.learning.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * An essay for one writing prompt of a published practice test. {@code essayText} is checked by the use case so an
 * empty or wrong-length essay gets its own error code.
 */
public record SubmitFreeWritingRequest(@NotNull UUID requestId, @NotNull UUID packageVersionId,
                                       @NotNull UUID questionVersionId, String essayText) {}
