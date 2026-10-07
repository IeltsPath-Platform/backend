package com.ieltspath.learning.api.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartPracticeAttemptRequest(@NotNull UUID packageId) {}
