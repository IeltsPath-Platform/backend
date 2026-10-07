package com.ieltspath.access.application.command;

import java.time.Instant;
import java.util.UUID;

public record GenerateActivationKeysCommand(
        UUID productId,
        int count,
        Instant expiresAt,
        UUID createdBy
        ) {

}
