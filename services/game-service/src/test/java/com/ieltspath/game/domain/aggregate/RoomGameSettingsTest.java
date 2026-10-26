package com.ieltspath.game.domain.aggregate;

import static org.junit.jupiter.api.Assertions.*;

import com.ieltspath.game.domain.vo.RoomGame;
import com.ieltspath.game.domain.vo.WordlyRoomSettings;
import com.ieltspath.game.domain.vo.WordlyRoomSettings.PlayMode;
import com.ieltspath.game.domain.vo.WordlyRoomSettings.PlayerMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoomGameSettingsTest {
    private final Instant now = Instant.parse("2026-10-22T00:00:00Z");

    @Test
    void openTakesTheGameTypeAndCapacityFromTheSettings() {
        var settings = new WordlyRoomSettings(PlayerMode.SOLO, 4, PlayMode.FINITE, 6);
        GameRoom room = GameRoom.open(UUID.randomUUID(), "ABC123", UUID.randomUUID(), settings, now);
        assertEquals("WORDLY", room.gameType());
        assertEquals(RoomGame.WORDLY, room.roomGame().orElseThrow());
        assertEquals("SOLO", room.playerMode());
        assertEquals(1, room.maxPlayers());
        assertSame(settings, room.settings());
        assertSame(settings, room.wordlySettings());
    }

    @Test
    void legacySessionRoomsAreNotRoomGames() {
        GameRoom room = new GameRoom(UUID.randomUUID(), "LEGACY", UUID.randomUUID(), 4, now);
        assertTrue(room.roomGame().isEmpty());
        assertNull(room.settings());
        assertEquals("MULTIPLAYER", room.playerMode());
        assertFalse(RoomGame.isRoomGame("WORD_MEANING_MATCH"));
        assertTrue(RoomGame.isRoomGame("ROCKET"));
        assertEquals(List.of("WORDLY", "ROCKET"), RoomGame.storedTypes());
    }

    @Test
    void reconstituteRejectsSettingsOfAnotherGame() {
        UUID host = UUID.randomUUID();
        var members = List.of(new GameRoom.Member(UUID.randomUUID(), host, GameRoom.MemberRole.HOST,
                GameRoom.MemberStatus.JOINED, now, 1));
        var settings = new WordlyRoomSettings(PlayerMode.MULTIPLAYER, 4, PlayMode.FINITE, 10);
        assertThrows(IllegalArgumentException.class, () -> GameRoom.reconstitute(UUID.randomUUID(), "ABC123", host, 4,
                "ROCKET", "VOCABULARY", "PRACTICE", Map.of(), now, now, null, GameRoom.GameRoomStatus.WAITING,
                members, settings));
    }
}
