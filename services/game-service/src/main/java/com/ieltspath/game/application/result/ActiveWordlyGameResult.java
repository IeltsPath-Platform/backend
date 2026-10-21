package com.ieltspath.game.application.result;

import java.util.UUID;

/** The Wordly room or match the caller still belongs to; matchId and playerStatus are null while waiting. */
public record ActiveWordlyGameResult(UUID roomId, String roomCode, String playerMode, String roomStatus,
        UUID matchId, String playerStatus) {
    public boolean playing() {
        return matchId != null && ("ACTIVE".equals(playerStatus) || "DISCONNECTED".equals(playerStatus));
    }
}
