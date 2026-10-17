package com.ieltspath.access.api.dto.response;

import java.util.UUID;

public record HumanGradingCreditResponse(UUID submissionId, int remainingCredits) {}
