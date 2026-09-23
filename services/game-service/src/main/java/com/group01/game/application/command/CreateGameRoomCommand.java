package com.group01.game.application.command;

import java.util.List;
import java.util.UUID;

public record CreateGameRoomCommand(UUID hostUserId, String gameType, String learningDomain, String mode,
                                   int maxPlayers, List<UUID> contentIds) {
    public CreateGameRoomCommand { contentIds = List.copyOf(contentIds); }
}
