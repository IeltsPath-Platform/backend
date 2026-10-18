package com.ieltspath.game.domain.aggregate;
import com.ieltspath.game.domain.vo.WordlyRoomSettings;
import com.ieltspath.game.domain.exception.GameRoomForbiddenException;
import com.ieltspath.game.domain.exception.InvalidGameRoomStateException;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class RoomSettingsTest {
    @Test void editsOnlyWaitingRoomAndResetsReadinessOnActualChange() {
        var host = UUID.randomUUID(); var guest = UUID.randomUUID();
        var room = GameRoom.wordly(UUID.randomUUID(), "ABC234", host,
            new WordlyRoomSettings(WordlyRoomSettings.PlayerMode.MULTIPLAYER, 4, WordlyRoomSettings.PlayMode.FINITE, 10), Instant.now());
        room.join(guest, Instant.now());
        room.setReady(host, true, Instant.now());room.setReady(guest, true, Instant.now());
        room.updateSettings(host, 4, 10, Instant.now());assertTrue(room.canStart());
        assertThrows(GameRoomForbiddenException.class, () -> room.updateSettings(guest, 3, 5, Instant.now()));
        assertThrows(IllegalArgumentException.class, () -> room.updateSettings(host, 2, 5, Instant.now()));
        assertThrows(IllegalArgumentException.class, () -> room.updateSettings(host, 7, 5, Instant.now()));
        assertTrue(room.canStart());
        room.updateSettings(host, 3, 5, Instant.now());
        assertFalse(room.canStart());assertEquals(3, room.wordlySettings().initialLives());
        assertEquals(5, room.wordlySettings().wordLimit());
        assertEquals(180, room.wordlySettings().wordTimeLimitSeconds());
        assertEquals("MULTIPLAYER", room.playerMode());assertEquals(4,room.maxPlayers());
        room.updateSettings(host, 6, 20, Instant.now());
        room.setReady(host, true, Instant.now());room.setReady(guest, true, Instant.now());
        room.startMatch(host, Instant.now());
        assertThrows(InvalidGameRoomStateException.class, () -> room.updateSettings(host, 3, 5, Instant.now()));
        assertEquals(6, room.wordlySettings().initialLives());
    }
    @Test void soloTimerCanBeDisabledAndEnabledWhileWaiting() {
        var host=UUID.randomUUID();
        var room=GameRoom.wordly(UUID.randomUUID(),"ABC234",host,
            new WordlyRoomSettings(WordlyRoomSettings.PlayerMode.SOLO,3,WordlyRoomSettings.PlayMode.FINITE,5,false),Instant.now());
        assertNull(room.wordlySettings().wordTimeLimitSeconds());
        room.updateSettings(host,6,10,null,Instant.now());
        assertFalse(room.wordlySettings().timeEnabled());
        room.updateSettings(host,6,10,true,Instant.now());
        assertEquals(180,room.wordlySettings().wordTimeLimitSeconds());
        assertThrows(IllegalArgumentException.class, () -> new WordlyRoomSettings(
            WordlyRoomSettings.PlayerMode.MULTIPLAYER,4,WordlyRoomSettings.PlayMode.FINITE,10,false));
        room.setReady(host,true,Instant.now()); room.startMatch(host,Instant.now());
        assertThrows(InvalidGameRoomStateException.class, () -> room.updateSettings(host,3,5,false,Instant.now()));
    }
    @Test void endlessModeCannotBeConvertedByUpdatingWordLimit() {
        var host=UUID.randomUUID();
        var room=GameRoom.wordly(UUID.randomUUID(),"ABC234",host,
            new WordlyRoomSettings(WordlyRoomSettings.PlayerMode.SOLO,4,WordlyRoomSettings.PlayMode.ENDLESS,null),Instant.now());
        assertThrows(IllegalArgumentException.class, () -> room.updateSettings(host,3,5,Instant.now()));
        room.updateSettings(host,3,null,Instant.now());assertNull(room.wordlySettings().wordLimit());
    }
}
