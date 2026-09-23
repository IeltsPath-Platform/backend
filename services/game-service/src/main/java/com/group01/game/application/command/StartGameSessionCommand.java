package com.group01.game.application.command;

import java.util.List;
import java.util.UUID;

public record StartGameSessionCommand(UUID userId, String gameType, String learningDomain, String mode,
                                      UUID topicId, List<UUID> contentIds) {
    public StartGameSessionCommand { contentIds = List.copyOf(contentIds); }
}
