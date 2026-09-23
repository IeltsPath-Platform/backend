package com.group01.assessment.api.dto.request;
import jakarta.validation.constraints.NotNull;
public record SaveAttemptResponseRequest(@NotNull String payload,int schemaVersion,long expectedRevision) {}
