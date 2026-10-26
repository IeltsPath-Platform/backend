package com.ieltspath.game.application.result;

import java.util.UUID;

/**
 * The room or match the caller still belongs to, in any room game; matchId and playerStatus are null while waiting.
 * The name predates Rocket: the record covers every room game and says which one in {@code gameType}.
 */
public record ActiveWordlyGameResult(UUID roomId, String roomCode, String playerMode, String roomStatus,
        UUID matchId, String playerStatus, String gameType) {
    public ActiveWordlyGameResult(UUID roomId, String roomCode, String playerMode, String roomStatus,
            UUID matchId, String playerStatus) {
        this(roomId, roomCode, playerMode, roomStatus, matchId, playerStatus, "WORDLY");
    }

    public boolean playing() {
        return matchId != null && ("ACTIVE".equals(playerStatus) || "DISCONNECTED".equals(playerStatus));
    }
}
