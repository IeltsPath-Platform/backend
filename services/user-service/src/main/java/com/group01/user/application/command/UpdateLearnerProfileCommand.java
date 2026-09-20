package com.group01.user.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateLearnerProfileCommand(
        UUID userId,
        String displayName,
        String avatarReference,
        String bio,
        BigDecimal selfReportedBand,
        String timezone,
        String visibility
) {
}

