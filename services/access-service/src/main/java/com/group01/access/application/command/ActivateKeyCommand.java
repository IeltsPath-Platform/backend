package com.group01.access.application.command;

import java.util.UUID;

public record ActivateKeyCommand(
        UUID userId,
        String rawKey,
        String idempotencyKey
        ) {

}
