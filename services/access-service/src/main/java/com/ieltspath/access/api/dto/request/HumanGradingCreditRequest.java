package com.ieltspath.access.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record HumanGradingCreditRequest(@NotNull UUID submissionId) {}
