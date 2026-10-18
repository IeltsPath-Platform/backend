package com.ieltspath.game.application.result;

import java.util.UUID;

public record WordlyPlayerLeftEvent(UUID roomId, UUID matchId, UUID userId, String displayName) {}
