package com.ieltspath.access.application.command;

import java.util.UUID;

public record RefundHumanGradingCreditCommand(UUID userId, UUID submissionId) {}
