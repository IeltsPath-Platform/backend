package com.ieltspath.learning.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * {@code partSectionIds} are the parts answered; empty means the whole test. {@code consent} must be true: the
 * recordings are sent to a speech service and a language model.
 */
public record CreateSpeakingSubmissionRequest(@NotNull UUID requestId, @NotNull UUID packageVersionId,
                                              List<UUID> partSectionIds, boolean consent) {
}
