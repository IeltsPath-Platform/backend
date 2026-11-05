package com.ieltspath.game.domain.aggregate;

import static org.junit.jupiter.api.Assertions.*;

import com.ieltspath.game.domain.exception.GameRoomForbiddenException;
import com.ieltspath.game.domain.vo.RocketRoomSettings;
import com.ieltspath.game.domain.vo.WordFilter;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoomWordFilterTest {
    private final Instant now = Instant.parse("2026-10-30T00:00:00Z");
    private final UUID host = UUID.randomUUID();
    private final UUID guest = UUID.randomUUID();
    private final WordFilter b1 = new WordFilter("B1", null);

    private GameRoom readyRoom() {
        var room = GameRoom.open(UUID.randomUUID(), "ABC123", host, RocketRoomSettings.multiplayer(10, 4, 15), now);
        room.join(guest, now);
        room.setReady(guest, true, now);
        return room;
    }

    @Test
    void changingTheFilterUnReadiesEveryone() {
        var room = readyRoom();
        room.updateRocketSettings(host, 4, null, null, b1, now);
        assertEquals(b1, room.settings().wordFilter());
        assertEquals(GameRoom.MemberStatus.JOINED, room.members().get(guest).status());
    }

    @Test
    void anUnchangedFilterLeavesPlayersReady() {
        var room = readyRoom();
        room.updateRocketSettings(host, 4, null, null, WordFilter.ANY, now);
        assertEquals(GameRoom.MemberStatus.READY, room.members().get(guest).status());
    }

    @Test
    void anyClearsTheFilterAndNullKeepsIt() {
        var room = readyRoom();
        room.updateRocketSettings(host, 4, null, null, b1, now);
        room.updateRocketSettings(host, 5, null, null, now);
        assertEquals(b1, room.settings().wordFilter());
        room.updateRocketSettings(host, 5, null, null, WordFilter.ANY, now);
        assertTrue(room.settings().wordFilter().isAny());
    }

    @Test
    void onlyTheHostChooses() {
        var room = readyRoom();
        assertThrows(GameRoomForbiddenException.class, () -> room.updateRocketSettings(guest, 4, null, null, b1, now));
    }

    @Test
    void rejectsAnUnknownLevel() {
        assertThrows(IllegalArgumentException.class, () -> new WordFilter("D1", null));
    }
}
