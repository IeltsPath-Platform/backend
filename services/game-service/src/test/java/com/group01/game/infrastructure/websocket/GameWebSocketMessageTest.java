package com.group01.game.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.game.application.result.GameAnswerResult;
import com.group01.game.application.result.GameItemResult;
import com.group01.game.application.result.GameMatchResult;
import com.group01.game.application.result.GameRoomResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GameWebSocketMessageTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void roomStateRetainsEnvelopeAndPublicRoomFieldsWithoutAnswerMaterial() {
        UUID roomId = UUID.randomUUID();
        var item = new GameItemResult("canonical", "sense", "version", "prompt", List.of("option"));
        var room = new GameRoomResult(roomId, "ABCD", UUID.randomUUID(), "QUIZ", "VOCABULARY", "SOLO", 2,
                "WAITING", Instant.EPOCH, List.of(), List.of(item));

        var json = objectMapper.valueToTree(RoomStateMessage.of("ROOM_STATE", room));

        assertEquals("ROOM_STATE", json.path("type").asText());
        assertEquals(roomId.toString(), json.path("room").path("id").asText());
        assertEquals("ABCD", json.path("room").path("roomCode").asText());
        assertEquals("prompt", json.path("room").path("items").get(0).path("prompt").asText());
        assertFalse(json.path("room").path("items").get(0).has("answerSpecJson"));
        assertFalse(json.path("room").path("items").get(0).has("explanation"));
    }

    @Test
    void matchAndAnswerMessagesKeepTheirExistingEnvelopeKeys() {
        UUID userId = UUID.randomUUID();
        var match = new GameMatchResult(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "IN_PROGRESS",
                Instant.EPOCH, List.of(new GameMatchResult.PlayerResult(userId, 1, null, "ACTIVE")));
        var answer = new GameAnswerResult(UUID.randomUUID(), 1, true, 1, "IN_PROGRESS", false);

        var matchJson = objectMapper.valueToTree(MatchStateMessage.of("MATCH_STATE", match));
        var answerJson = objectMapper.valueToTree(AnswerAcceptedMessage.of(userId.toString(), answer));

        assertEquals("MATCH_STATE", matchJson.path("type").asText());
        org.junit.jupiter.api.Assertions.assertTrue(matchJson.has("match"));
        assertEquals("ANSWER_ACCEPTED", answerJson.path("type").asText());
        assertEquals(userId.toString(), answerJson.path("userId").asText());
        org.junit.jupiter.api.Assertions.assertTrue(answerJson.has("answer"));
    }
}
