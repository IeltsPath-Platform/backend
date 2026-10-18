package com.ieltspath.game.application.command;

import com.ieltspath.game.domain.vo.WordlyRoomSettings;
import java.util.Objects;
import java.util.UUID;

public record CreateWordlyRoomCommand(UUID hostUserId, WordlyRoomSettings settings) {
    public CreateWordlyRoomCommand {
        Objects.requireNonNull(hostUserId, "Host is required");
        Objects.requireNonNull(settings, "Settings are required");
    }
}
