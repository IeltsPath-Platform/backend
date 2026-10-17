package com.ieltspath.game.application.result;
import java.util.UUID;
public record MatchStartedEvent(UUID roomId, UUID matchId) {}
