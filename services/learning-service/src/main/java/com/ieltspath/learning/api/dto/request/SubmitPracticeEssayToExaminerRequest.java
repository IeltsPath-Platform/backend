package com.ieltspath.learning.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * An essay of a practice attempt, sent to the examiner the learner chose instead of the AI. {@code essayText} is checked by
 * the use case so an empty or wrong-length essay gets its own error code.
 */
public record SubmitPracticeEssayToExaminerRequest(@NotNull UUID requestId, String essayText, @NotNull UUID examinerId) {}
