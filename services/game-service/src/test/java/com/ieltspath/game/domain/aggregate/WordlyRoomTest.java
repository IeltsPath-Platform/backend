package com.ieltspath.game.domain.aggregate;

import com.ieltspath.game.domain.vo.WordlyRoomSettings;
import com.ieltspath.game.domain.exception.InvalidGameRoomStateException;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WordlyRoomTest {
    @Test
    void soloRoomIsPrivateAndRetainsEndlessSettings() {
        var settings = new WordlyRoomSettings(WordlyRoomSettings.PlayerMode.SOLO, 3, WordlyRoomSettings.PlayMode.ENDLESS, null);
        var room = GameRoom.wordly(UUID.randomUUID(), "AB7K2Q", UUID.randomUUID(), settings, Instant.now());
        assertEquals(1, room.maxPlayers());
        assertEquals("SOLO", room.playerMode());
        assertEquals(settings, room.wordlySettings());
        assertThrows(InvalidGameRoomStateException.class, () -> room.join(UUID.randomUUID(), Instant.now()));
    }

    @Test
    void multiplayerAllocatesFourSlotsAndRejectsFifthPlayer() {
        var settings = new WordlyRoomSettings(WordlyRoomSettings.PlayerMode.MULTIPLAYER, 6, WordlyRoomSettings.PlayMode.FINITE, 5);
        var room = GameRoom.wordly(UUID.randomUUID(), "AB7K2Q", UUID.randomUUID(), settings, Instant.now());
        for (int i = 0; i < 3; i++) room.join(UUID.randomUUID(), Instant.now());
        assertEquals(4, room.members().values().stream().map(GameRoom.Member::slotNumber).distinct().count());
        assertThrows(InvalidGameRoomStateException.class, () -> room.join(UUID.randomUUID(), Instant.now()));
    }

    @Test
    void hostLeavingClosesRoomAndPreventsFurtherJoin() {
        var host = UUID.randomUUID();
        var settings = new WordlyRoomSettings(WordlyRoomSettings.PlayerMode.MULTIPLAYER, 4, WordlyRoomSettings.PlayMode.FINITE, 10);
        var room = GameRoom.wordly(UUID.randomUUID(), "AB7K2Q", host, settings, Instant.now());
        room.join(UUID.randomUUID(), Instant.now());
        room.leave(host, Instant.now());
        assertEquals(GameRoom.GameRoomStatus.CLOSED, room.status());
        assertThrows(InvalidGameRoomStateException.class, () -> room.join(UUID.randomUUID(), Instant.now()));
    }

    @Test
    void startRequiresHostTwoMembersAndEveryoneReady() {
        var host = UUID.randomUUID(); var guest = UUID.randomUUID();
        var settings = new WordlyRoomSettings(WordlyRoomSettings.PlayerMode.MULTIPLAYER, 4, WordlyRoomSettings.PlayMode.FINITE, 10);
        var room = GameRoom.wordly(UUID.randomUUID(), "AB7K2Q", host, settings, Instant.now());
        room.setReady(host, true, Instant.now());
        assertFalse(room.canStart());
        assertThrows(InvalidGameRoomStateException.class, () -> room.startMatch(host, Instant.now()));
        room.join(guest, Instant.now());
        assertFalse(room.canStart());
        room.setReady(guest, true, Instant.now());
        assertTrue(room.canStart());
        assertThrows(InvalidGameRoomStateException.class, () -> room.startMatch(guest, Instant.now()));
        room.setReady(guest, false, Instant.now()); assertFalse(room.canStart());
        room.setReady(guest, true, Instant.now()); room.startMatch(host, Instant.now());
        assertFalse(room.canStart());
        assertThrows(InvalidGameRoomStateException.class, () -> room.join(UUID.randomUUID(), Instant.now()));
        assertThrows(InvalidGameRoomStateException.class, () -> room.startMatch(host, Instant.now()));
    }

    @Test
    void rejectsInvalidSettings() {
        assertThrows(IllegalArgumentException.class, () -> new WordlyRoomSettings(WordlyRoomSettings.PlayerMode.SOLO, 2, WordlyRoomSettings.PlayMode.FINITE, 5));
        assertThrows(IllegalArgumentException.class, () -> new WordlyRoomSettings(WordlyRoomSettings.PlayerMode.SOLO, 4, WordlyRoomSettings.PlayMode.FINITE, 4));
        assertThrows(IllegalArgumentException.class, () -> new WordlyRoomSettings(WordlyRoomSettings.PlayerMode.SOLO, 4, WordlyRoomSettings.PlayMode.ENDLESS, 5));
    }
}
