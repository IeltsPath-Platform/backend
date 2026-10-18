package com.ieltspath.access.api.dto.response;

import java.util.UUID;

/** {@code charged} is true when this call spent a credit and false when the submission's credit was already spent. */
public record HumanGradingCreditResponse(UUID submissionId, int remainingCredits, boolean charged) {}
