package com.ieltspath.game.domain.aggregate;

import com.ieltspath.game.domain.exception.GameRoomForbiddenException;
import com.ieltspath.game.domain.exception.InvalidGameRoomStateException;
import com.ieltspath.game.domain.vo.WordlyRoomSettings;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class RoomLobbyMembershipTest {
    private final UUID host = UUID.randomUUID();
    private final UUID guest = UUID.randomUUID();
    private final Instant now = Instant.EPOCH;

    private GameRoom room() {
        var room = GameRoom.wordly(UUID.randomUUID(), "AB7K2Q", host,
                new WordlyRoomSettings(WordlyRoomSettings.PlayerMode.MULTIPLAYER, 4,
                        WordlyRoomSettings.PlayMode.FINITE, 10), now);
        room.join(guest, now);
        return room;
    }

    @Test
    void kickRetainsMemberHistoryAndReleasesSlot() {
        var room = room();
        room.setMemberDisplayName(guest, "Guest");
        var member = room.members().get(guest);
        room.kick(host, guest, now.plusSeconds(1));
        assertFalse(room.hasActiveMember(guest));
        assertEquals(GameRoom.MemberStatus.LEFT, room.members().get(guest).status());
        assertEquals(member.id(), room.members().get(guest).id());
        assertEquals("Guest", room.members().get(guest).displayName());
        var replacement = UUID.randomUUID();
        room.join(replacement, now.plusSeconds(2));
        assertEquals(member.slotNumber(), room.members().get(replacement).slotNumber());
        assertEquals(GameRoom.GameRoomStatus.WAITING, room.status());
    }

    @Test
    void kickRequiresHostAndActiveOtherMember() {
        var room = room();
        assertThrows(GameRoomForbiddenException.class, () -> room.kick(guest, host, now));
        assertThrows(InvalidGameRoomStateException.class, () -> room.kick(host, host, now));
        assertThrows(InvalidGameRoomStateException.class, () -> room.kick(host, UUID.randomUUID(), now));
        room.kick(host, guest, now);
        assertThrows(InvalidGameRoomStateException.class, () -> room.kick(host, guest, now));
    }

    @Test
    void activeMatchRejectsLobbyKickLeaveAndCloseWithoutChangingMembership() {
        var room = room();
        room.setReady(host, true, now);
        room.setReady(guest, true, now);
        room.startMatch(host, now);
        assertThrows(InvalidGameRoomStateException.class, () -> room.kick(host, guest, now));
        assertThrows(InvalidGameRoomStateException.class, () -> room.leave(host, now));
        assertThrows(InvalidGameRoomStateException.class, () -> room.leave(guest, now));
        assertThrows(InvalidGameRoomStateException.class, () -> room.close(host, now));
        assertTrue(room.hasActiveMember(host));
        assertTrue(room.hasActiveMember(guest));
        assertEquals(GameRoom.GameRoomStatus.IN_MATCH, room.status());
    }
}
