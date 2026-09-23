package com.group01.game.infrastructure.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.game.application.result.GameMatchResult;
import com.group01.game.application.usecase.ChangeGameRoomMembershipUseCase;
import com.group01.game.application.usecase.GetGameMatchStateUseCase;
import com.group01.game.application.usecase.GetGameRoomUseCase;
import com.group01.game.application.usecase.StartGameMatchUseCase;
import com.group01.game.application.usecase.SubmitGameAnswerUseCase;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    public static final String ROOM_ID_ATTRIBUTE = "gameRoomId";
    public static final String USER_ID_ATTRIBUTE = "gameUserId";
    private static final int MAX_MESSAGE_CHARS = 16_384;

    private final ObjectMapper objectMapper;
    private final GetGameRoomUseCase getRoom;
    private final ChangeGameRoomMembershipUseCase membership;
    private final StartGameMatchUseCase startMatch;
    private final GetGameMatchStateUseCase getMatch;
    private final SubmitGameAnswerUseCase submitAnswer;
    private final Map<UUID, Set<WebSocketSession>> sessionsByRoom = new ConcurrentHashMap<>();

    public GameWebSocketHandler(ObjectMapper objectMapper, GetGameRoomUseCase getRoom,
                                ChangeGameRoomMembershipUseCase membership, StartGameMatchUseCase startMatch,
                                GetGameMatchStateUseCase getMatch, SubmitGameAnswerUseCase submitAnswer) {
        this.objectMapper = objectMapper;
        this.getRoom = getRoom;
        this.membership = membership;
        this.startMatch = startMatch;
        this.getMatch = getMatch;
        this.submitAnswer = submitAnswer;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        UUID roomId = attribute(session, ROOM_ID_ATTRIBUTE);
        UUID userId = attribute(session, USER_ID_ATTRIBUTE);
        session.setTextMessageSizeLimit(MAX_MESSAGE_CHARS);
        sessionsByRoom.computeIfAbsent(roomId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        send(session, Map.of("type", "CONNECTED", "roomId", roomId.toString(), "userId", userId.toString()));
        broadcastRoomState(roomId, userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        if (message.getPayloadLength() > MAX_MESSAGE_CHARS) {
            session.close(CloseStatus.TOO_BIG_TO_PROCESS);
            return;
        }
        UUID roomId = attribute(session, ROOM_ID_ATTRIBUTE);
        UUID userId = attribute(session, USER_ID_ATTRIBUTE);
        try {
            JsonNode envelope = objectMapper.readTree(message.getPayload());
            String type = envelope.path("type").asText("");
            switch (type) {
                case "PING" -> send(session, Map.of("type", "PONG"));
                case "ROOM_STATE" -> send(session, Map.of("type", "ROOM_STATE", "room", getRoom.byId(roomId, userId)));
                case "RECONNECT" -> reconnect(session, userId, requiredUuid(envelope, "matchId"));
                case "READY" -> {
                    membership.ready(roomId, userId, envelope.path("ready").asBoolean(false));
                    broadcastRoomState(roomId, userId);
                }
                case "START_MATCH" -> {
                    GameMatchResult started = startMatch.execute(roomId, userId);
                    broadcastPersonalizedMatchState(roomId, started.matchId(), "MATCH_STARTED");
                }
                case "ANSWER" -> submitMatchAnswer(envelope, roomId, userId);
                case "LEAVE" -> {
                    membership.leave(roomId, userId);
                    broadcast(roomId, Map.of("type", "MEMBER_LEFT", "userId", userId.toString()));
                    session.close(CloseStatus.NORMAL);
                }
                default -> send(session, Map.of("type", "ERROR", "message", "Unsupported message type"));
            }
        } catch (RuntimeException exception) {
            send(session, Map.of("type", "ERROR", "message", "Invalid or unauthorized game message"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object roomValue = session.getAttributes().get(ROOM_ID_ATTRIBUTE);
        if (roomValue instanceof UUID roomId) {
            Set<WebSocketSession> members = sessionsByRoom.get(roomId);
            if (members != null) {
                members.remove(session);
                if (members.isEmpty()) sessionsByRoom.remove(roomId, members);
            }
        }
    }

    private void reconnect(WebSocketSession session, UUID userId, UUID matchId) throws IOException {
        send(session, Map.of("type", "MATCH_STATE", "match", getMatch.execute(matchId, userId)));
    }

    private void submitMatchAnswer(JsonNode envelope, UUID roomId, UUID userId) throws IOException {
        UUID matchId = requiredUuid(envelope, "matchId");
        GameMatchResult ownState = getMatch.execute(matchId, userId);
        if (!roomId.equals(ownState.roomId())) throw new IllegalArgumentException("Match belongs to another room");
        if (envelope.hasNonNull("sessionId")
                && !ownState.sessionId().equals(requiredUuid(envelope, "sessionId"))) {
            throw new IllegalArgumentException("Session does not belong to this player and match");
        }
        int sequence = envelope.path("itemSequence").asInt(-1);
        long duration = envelope.path("durationMilliseconds").asLong(-1);
        JsonNode answerNode = envelope.path("responsePayload");
        if (!answerNode.isObject() || !answerNode.has("answer")) {
            throw new IllegalArgumentException("responsePayload.answer is required");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.convertValue(answerNode, Map.class);
        var result = submitAnswer.execute(ownState.sessionId(), userId, sequence, payload, duration);
        broadcast(ownState.roomId(), Map.of("type", "ANSWER_ACCEPTED", "userId", userId.toString(), "answer", result));
        if ("COMPLETED".equals(result.sessionStatus())) {
            broadcastPersonalizedMatchState(ownState.roomId(), matchId, "MATCH_STATE");
        }
    }

    private void broadcastPersonalizedMatchState(UUID roomId, UUID matchId, String type) throws IOException {
        Set<WebSocketSession> members = sessionsByRoom.getOrDefault(roomId, Set.of());
        for (WebSocketSession member : members) {
            if (!member.isOpen()) continue;
            UUID userId = attribute(member, USER_ID_ATTRIBUTE);
            try {
                send(member, Map.of("type", type, "match", getMatch.execute(matchId, userId)));
            } catch (RuntimeException ignored) {
                // A connected room member who is not a match player must not receive match data.
            }
        }
    }

    private void broadcastRoomState(UUID roomId, UUID userId) throws IOException {
        var room = getRoom.byId(roomId, userId);
        broadcast(roomId, Map.of("type", "ROOM_STATE", "room", room));
    }

    private void broadcast(UUID roomId, Object value) throws IOException {
        String payload = objectMapper.writeValueAsString(value);
        Set<WebSocketSession> members = sessionsByRoom.getOrDefault(roomId, Set.of());
        for (WebSocketSession member : members) {
            if (member.isOpen()) member.sendMessage(new TextMessage(payload));
        }
    }

    private void send(WebSocketSession session, Object value) throws IOException {
        if (session.isOpen()) session.sendMessage(new TextMessage(objectMapper.writeValueAsString(value)));
    }

    private UUID requiredUuid(JsonNode envelope, String field) {
        JsonNode value = envelope.get(field);
        if (value == null || !value.isTextual()) throw new IllegalArgumentException(field + " is required");
        return UUID.fromString(value.asText());
    }

    private UUID attribute(WebSocketSession session, String name) {
        Object value = session.getAttributes().get(name);
        if (!(value instanceof UUID id)) throw new IllegalStateException("WebSocket ticket is missing its identity");
        return id;
    }
}
