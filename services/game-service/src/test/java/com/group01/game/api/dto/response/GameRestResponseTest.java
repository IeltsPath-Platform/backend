package com.group01.game.api.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.game.application.result.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GameRestResponseTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void websocketTicketResponseKeepsExistingFields() {
        var response = GameTicketResponse.from(new GameTicketResult("ticket-value", Instant.EPOCH,
                List.of("game.v1", "ticket.ticket-value")));

        var json = objectMapper.valueToTree(response);

        assertEquals("ticket-value", json.path("ticket").asText());
        assertTrue(json.has("expiresAt"));
        assertEquals(2, json.path("subprotocols").size());
    }

    @Test
    void historyResponseKeepsContentAndPaginationFields() {
        var session = new GameSessionResult(UUID.randomUUID(), "QUIZ", "VOCABULARY", "SOLO", null,
                "COMPLETED", 2, 2, 2, Instant.EPOCH, Instant.EPOCH, List.of());
        var response = GameHistoryResponse.from(new GameHistoryResult(List.of(session), 1, 20, 21, 2));

        var json = objectMapper.valueToTree(response);

        assertEquals(1, json.path("page").asInt());
        assertEquals(20, json.path("size").asInt());
        assertEquals(21, json.path("totalElements").asInt());
        assertEquals(2, json.path("totalPages").asInt());
        assertEquals("QUIZ", json.path("content").get(0).path("gameType").asText());
    }

    @Test
    void roomSessionMatchAndAnswerResponsesKeepPublicFieldsOnly() {
        var item = new GameItemResult("canonical", null, "version", "prompt", List.of("option"));
        var roomResult = new GameRoomResult(UUID.randomUUID(), "ABCD", UUID.randomUUID(), "QUIZ", "VOCABULARY",
                "SOLO", 2, "WAITING", Instant.EPOCH,
                List.of(new GameRoomResult.MemberResult(UUID.randomUUID(), "PLAYER", "READY", Instant.EPOCH)),
                List.of(item));
        var sessionResult = new GameSessionResult(UUID.randomUUID(), "QUIZ", "VOCABULARY", "SOLO", null,
                "PENDING", 0, 1, 0, Instant.EPOCH, null, List.of(item));
        var player = new GameMatchResult.PlayerResult(UUID.randomUUID(), 0, null, "ACTIVE");
        var matchResult = new GameMatchResult(UUID.randomUUID(), roomResult.id(), sessionResult.id(),
                "IN_PROGRESS", Instant.EPOCH, List.of(player));

        var room = objectMapper.valueToTree(GameRoomResponse.from(roomResult));
        var session = objectMapper.valueToTree(GameSessionResponse.from(sessionResult));
        var match = objectMapper.valueToTree(GameMatchResponse.from(matchResult));
        var answer = objectMapper.valueToTree(GameAnswerResponse.from(
                new GameAnswerResult(UUID.randomUUID(), 1, true, 1, "IN_PROGRESS", false)));

        assertEquals("ABCD", room.path("roomCode").asText());
        assertEquals("READY", room.path("members").get(0).path("status").asText());
        assertEquals("canonical", room.path("items").get(0).path("canonicalId").asText());
        assertEquals("QUIZ", session.path("gameType").asText());
        assertEquals("prompt", session.path("items").get(0).path("prompt").asText());
        assertFalse(session.path("items").get(0).has("answerSpecJson"));
        assertEquals("IN_PROGRESS", match.path("status").asText());
        assertEquals("ACTIVE", match.path("players").get(0).path("status").asText());
        assertTrue(answer.path("correct").asBoolean());
    }
}
