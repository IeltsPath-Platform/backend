package com.ieltspath.game.domain.aggregate;

import com.ieltspath.game.domain.vo.WordlyRoomSettings;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import com.ieltspath.game.domain.exception.InvalidGameRoomStateException;
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

    @Test
    void failedStartWithAnUnreadyMemberKeepsRoomWaiting() {
        UUID host = UUID.randomUUID();
        UUID guest = UUID.randomUUID();
        GameRoom room = new GameRoom(UUID.randomUUID(), "ABCD", host, 2, Instant.EPOCH);
        room.join(guest, Instant.EPOCH);
        room.setReady(host, true, Instant.EPOCH.plusSeconds(1));

        var exception = assertThrows(InvalidGameRoomStateException.class,
                () -> room.startMatch(host, Instant.EPOCH.plusSeconds(2)));

        assertEquals("Every active member must be ready before starting", exception.getMessage());
        assertEquals(GameRoom.GameRoomStatus.WAITING, room.status());
        assertEquals(2, room.members().size());
    }

    @Test
    void aRoomCodeMustHaveSixUpperCaseCharacters() {
        for (String code : new String[]{"ABC12", "abc123"}) {
            var error = assertThrows(IllegalArgumentException.class,
                    () -> GameRoom.wordly(UUID.randomUUID(), code, UUID.randomUUID(), new WordlyRoomSettings(
                            WordlyRoomSettings.PlayerMode.MULTIPLAYER, 4, WordlyRoomSettings.PlayMode.FINITE, 5), Instant.EPOCH));
            assertEquals("Room code must have 6 characters", error.getMessage());
        }
    }

    @Test
    void aUserWhoIsNotInTheRoomCannotSetReady() {
        GameRoom room = new GameRoom(UUID.randomUUID(), "ABCD12", UUID.randomUUID(), 2, Instant.EPOCH);
        var error = assertThrows(InvalidGameRoomStateException.class,
                () -> room.setReady(UUID.randomUUID(), true, Instant.EPOCH));
        assertEquals("User is not an active room member", error.getMessage());
    }

    @Test
    void aGuestLeavingAWaitingRoomFreesTheirSlot() {
        UUID host = UUID.randomUUID();
        UUID guest = UUID.randomUUID();
        GameRoom room = GameRoom.wordly(UUID.randomUUID(), "ABCD12", host, new WordlyRoomSettings(
                WordlyRoomSettings.PlayerMode.MULTIPLAYER, 4, WordlyRoomSettings.PlayMode.FINITE, 5), Instant.EPOCH);
        room.join(guest, Instant.EPOCH);
        int slot = room.members().get(guest).slotNumber();

        room.leave(guest, Instant.EPOCH.plusSeconds(1));

        assertFalse(room.hasActiveMember(guest));
        assertEquals(GameRoom.GameRoomStatus.WAITING, room.status());
        UUID replacement = UUID.randomUUID();
        room.join(replacement, Instant.EPOCH.plusSeconds(2));
        assertEquals(slot, room.members().get(replacement).slotNumber());
        assertTrue(room.hasActiveMember(replacement));
    }
}
