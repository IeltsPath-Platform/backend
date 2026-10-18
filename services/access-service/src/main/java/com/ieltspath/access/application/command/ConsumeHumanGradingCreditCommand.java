package com.ieltspath.access.application.command;

import java.util.UUID;

public record ConsumeHumanGradingCreditCommand(
        UUID userId,
        UUID submissionId
        ) {

    public ConsumeHumanGradingCreditCommand(UUID userId) {
        this(userId, null);
    }

}
