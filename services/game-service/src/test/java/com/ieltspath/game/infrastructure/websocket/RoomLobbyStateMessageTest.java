package com.ieltspath.game.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.game.api.dto.response.GameRoomResponse;
import com.ieltspath.game.application.result.GameRoomResult;
import com.ieltspath.game.domain.aggregate.GameRoom;
import com.ieltspath.game.domain.vo.WordlyRoomSettings;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class RoomLobbyStateMessageTest {
    @Test
    void websocketSettingsSlotsAndReadinessMatchRestSnapshot() {
        var host = UUID.randomUUID();
        var room = GameRoom.wordly(UUID.randomUUID(), "AB7K2Q", host,
                new WordlyRoomSettings(WordlyRoomSettings.PlayerMode.MULTIPLAYER, 4,
                        WordlyRoomSettings.PlayMode.FINITE, 10), Instant.EPOCH);
        var guest = UUID.randomUUID();
        room.join(guest, Instant.EPOCH);
        room.setReady(host, true, Instant.EPOCH);
        room.setReady(guest, true, Instant.EPOCH);
        var result = GameRoomResult.from(room);
        var mapper = new ObjectMapper().findAndRegisterModules();
        var ws = mapper.valueToTree(RoomStateMessage.of("ROOM_STATE", result)).path("room");
        var rest = mapper.valueToTree(GameRoomResponse.from(result));
        assertEquals(rest.path("playerMode"), ws.path("playerMode"));
        assertEquals(rest.path("settings"), ws.path("settings"));
        assertEquals(rest.path("canStart"), ws.path("canStart"));
        assertTrue(ws.path("canStart").asBoolean());
        assertEquals(rest.path("members").get(1).path("slotNumber"), ws.path("members").get(1).path("slotNumber"));
    }
}
