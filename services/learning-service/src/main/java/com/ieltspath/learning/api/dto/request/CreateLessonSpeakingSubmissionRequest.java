package com.ieltspath.learning.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Opens the recorded answer of a lesson's practice attempt. {@code consent} must be true: the recording is sent to a
 * speech service and a language model.
 */
public record CreateLessonSpeakingSubmissionRequest(@NotNull UUID requestId, boolean consent) {
}
