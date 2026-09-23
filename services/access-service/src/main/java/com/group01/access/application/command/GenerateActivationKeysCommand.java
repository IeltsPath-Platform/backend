package com.group01.access.application.command;

import java.time.Instant;
import java.util.UUID;

public record GenerateActivationKeysCommand(
        UUID productId,
        int count,
        Instant expiresAt,
        UUID createdBy
        ) {

}
