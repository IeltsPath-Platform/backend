package com.group01.assessment.api.dto.request;

import com.group01.assessment.domain.vo.AttemptChannel;
import com.group01.assessment.domain.vo.AttemptMode;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Sections, items, attempt type and deadline come from Content, never from the client. Older clients may still
 * send {@code attemptType}, {@code expiresAt} or {@code sections}; Jackson ignores those unknown fields.
 */
public record StartAssessmentAttemptRequest(@NotNull UUID packageVersionId, @NotNull AttemptMode mode,
                                            @NotNull AttemptChannel channel) {}
