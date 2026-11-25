package com.ieltspath.access.api.dto.response;

import java.util.UUID;

/** {@code charged} is true when this call spent a credit; unlimited access and repeats return false. */
public record HumanGradingCreditResponse(UUID submissionId, int remainingCredits, boolean charged) {}
