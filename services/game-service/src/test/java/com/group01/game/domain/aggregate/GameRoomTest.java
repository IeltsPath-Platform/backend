package com.group01.game.domain.aggregate;

import com.group01.game.domain.exception.InvalidGameRoomStateException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameRoomTest {
    @Test
    void preventsCapacityOverflowAndRequiresHostAndReadyPlayersToStart() {
        UUID host = UUID.randomUUID();
        UUID guest = UUID.randomUUID();
        GameRoom room = new GameRoom(UUID.randomUUID(), "ABCD", host, 2, Instant.EPOCH);
        room.join(guest, Instant.EPOCH);
        assertThrows(InvalidGameRoomStateException.class, () -> room.join(UUID.randomUUID(), Instant.EPOCH));
        assertThrows(InvalidGameRoomStateException.class, () -> room.startMatch(host, Instant.EPOCH));
        room.setReady(host, true, Instant.EPOCH.plusSeconds(1));
        room.setReady(guest, true, Instant.EPOCH.plusSeconds(1));
        room.startMatch(host, Instant.EPOCH.plusSeconds(1));
        assertEquals(GameRoom.GameRoomStatus.IN_MATCH, room.status());
    }
}
