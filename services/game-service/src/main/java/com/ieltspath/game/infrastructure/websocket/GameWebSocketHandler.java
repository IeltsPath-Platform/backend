package com.ieltspath.game.infrastructure.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.game.application.result.GameMatchResult;
import com.ieltspath.game.application.usecase.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler implements org.springframework.web.socket.SubProtocolCapable {
    @Override
    public java.util.List<String> getSubProtocols() { return java.util.List.of("game.v1"); }
    public static final String ROOM_ID_ATTRIBUTE = "gameRoomId";
    public static final String USER_ID_ATTRIBUTE = "gameUserId";
    private static final int MAX_MESSAGE_CHARS = 16_384;

    private final ObjectMapper objectMapper;
    private final GetGameRoomUseCase getRoom;
    private final ChangeGameRoomMembershipUseCase membership;
    private final StartGameMatchUseCase startMatch;
    private final GetGameMatchStateUseCase getMatch;
    private final SubmitGameAnswerUseCase submitAnswer;
    private final SoloGameplayUseCase gameplay;
    private final GetWordlyRankingUseCase ranking;
    private final com.ieltspath.game.domain.repository.GameMatchRepository matches;
    private final Map<UUID, Set<WebSocketSession>> sessionsByRoom = new ConcurrentHashMap<>();
    private final Map<String,org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator> writers=new ConcurrentHashMap<>();
    private final Map<String,Long> lastPong=new ConcurrentHashMap<>();

    public GameWebSocketHandler(ObjectMapper objectMapper, GetGameRoomUseCase getRoom,
                                ChangeGameRoomMembershipUseCase membership, StartGameMatchUseCase startMatch,
                                GetGameMatchStateUseCase getMatch, SubmitGameAnswerUseCase submitAnswer,
                                SoloGameplayUseCase gameplay, GetWordlyRankingUseCase ranking,
                                com.ieltspath.game.domain.repository.GameMatchRepository matches) {
        this.objectMapper = objectMapper;
        this.getRoom = getRoom;
        this.membership = membership;
        this.startMatch = startMatch;
        this.getMatch = getMatch;
        this.submitAnswer = submitAnswer;
        this.gameplay=gameplay;
        this.ranking=ranking;
        this.matches=matches;
    }

    @org.springframework.transaction.event.TransactionalEventListener
    public void roomClosed(com.ieltspath.game.application.result.RoomClosedEvent event) {
        try {
            broadcast(event.roomId(), Map.of("type", "ROOM_CLOSED", "roomId", event.roomId().toString(), "message", event.message()));
            for (WebSocketSession member : sessionsByRoom.getOrDefault(event.roomId(), Set.of())) {
                closeQuietly(member,CloseStatus.NORMAL);
            }
            sessionsByRoom.remove(event.roomId());
        } catch (IOException exception) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Could not deliver room closure for {}", event.roomId());
        }
    }

    @org.springframework.transaction.event.TransactionalEventListener
    public void roomUpdated(com.ieltspath.game.application.result.RoomUpdatedEvent event) {
        try {
            for (var member : event.room().members()) {
                if ("LEFT".equals(member.status())) revoke(event.room().id(),member.userId(),"MEMBER_LEFT","Người chơi đã rời phòng");
            }
            broadcast(event.room().id(), RoomStateMessage.of("ROOM_STATE", event.room()));
        }
        catch (IOException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Room update failed for {}", event.room().id()); }
    }

    @org.springframework.transaction.event.TransactionalEventListener
    public void memberKicked(com.ieltspath.game.application.result.RoomMemberKickedEvent event) {
        try { revoke(event.roomId(), event.userId(), "PLAYER_KICKED", "Bạn đã bị chủ phòng mời ra khỏi phòng"); }
        catch (IOException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Kick notification failed for {}", event.roomId()); }
    }

    private void revoke(UUID roomId, UUID userId, String type, String message) throws IOException {
        for (var session : sessionsByRoom.getOrDefault(roomId, Set.of())) {
            if (!userId.equals(attribute(session, USER_ID_ATTRIBUTE))) continue;
            try { send(session,Map.of("type",type,"userId",userId,"message",message)); }
            finally { closeQuietly(session,CloseStatus.NORMAL); }
        }
    }

    @org.springframework.transaction.event.TransactionalEventListener
    public void wordlyUpdated(com.ieltspath.game.application.result.WordlyMatchUpdatedEvent event) {
        try {
            var state=ranking.execute(event.matchId(),event.userId());
            broadcastRanking(event.roomId(),state,"RANKING_UPDATED");
            if (event.finished()) {
                broadcastRanking(event.roomId(),state,"MATCH_FINISHED");
                if (event.closureMessage()!=null)
                    roomClosed(new com.ieltspath.game.application.result.RoomClosedEvent(event.roomId(),event.closureMessage()));
                else {
                    for (var session:sessionsByRoom.getOrDefault(event.roomId(),Set.of())) {
                        try { broadcastRoomState(event.roomId(),attribute(session,USER_ID_ATTRIBUTE)); break; }
                        catch (RuntimeException ignored) { /* A player who left cannot read the lobby. */ }
                    }
                }
            }
        } catch (IOException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Ranking update failed for {}",event.matchId()); }
    }

    private void broadcastRanking(UUID roomId,com.ieltspath.game.application.result.WordlyRankingResult state,String type) throws IOException {
        var users=state.players().stream().map(com.ieltspath.game.application.result.WordlyRankingResult.Player::userId).collect(java.util.stream.Collectors.toSet());
        for(var session:sessionsByRoom.getOrDefault(roomId,Set.of())) {
            if(users.contains(attribute(session,USER_ID_ATTRIBUTE))) {
                try { send(session,Map.of("type",type,"matchId",state.matchId(),"ranking",state)); }
                catch(IOException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Ranking socket delivery failed for {}",state.matchId()); }
            }
        }
    }

    @org.springframework.transaction.event.TransactionalEventListener
    public void matchStarted(com.ieltspath.game.application.result.MatchStartedEvent event) {
        try { broadcastPersonalizedMatchState(event.roomId(), event.matchId(), "MATCH_STARTED"); }
        catch (IOException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Match update failed for {}", event.matchId()); }
    }

    @org.springframework.transaction.event.TransactionalEventListener
    public void soloUpdated(com.ieltspath.game.application.result.SoloGameplayEvent event) {
        for(WebSocketSession session:sessionsByRoom.getOrDefault(event.roomId(),Set.of())) {
            if(!event.userId().equals(attribute(session,USER_ID_ATTRIBUTE)))continue;
            try{send(session,event.payload());}catch(IOException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Solo update failed for {}",event.roomId());}
        }
    }

    @org.springframework.transaction.event.TransactionalEventListener
    public void playerLeft(com.ieltspath.game.application.result.WordlyPlayerLeftEvent event) {
        try {
            var state=ranking.execute(event.matchId(),event.userId());
            var users=state.players().stream().map(com.ieltspath.game.application.result.WordlyRankingResult.Player::userId).collect(java.util.stream.Collectors.toSet());
            for(var session:sessionsByRoom.getOrDefault(event.roomId(),Set.of())) {
                if(users.contains(attribute(session,USER_ID_ATTRIBUTE)))
                    send(session,Map.of("type","MEMBER_LEFT","matchId",event.matchId(),"userId",event.userId(),
                        "displayName",event.displayName(),"message",event.displayName()+" đã rời phòng"));
            }
        } catch(IOException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Player leave delivery failed for {}",event.matchId()); }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        UUID roomId = attribute(session, ROOM_ID_ATTRIBUTE);
        UUID userId = attribute(session, USER_ID_ATTRIBUTE);
        session.setTextMessageSizeLimit(MAX_MESSAGE_CHARS);
        lastPong.put(session.getId(),System.currentTimeMillis());
        var sessions=sessionsByRoom.computeIfAbsent(roomId, ignored -> ConcurrentHashMap.newKeySet());
        synchronized(sessions) {
            sessions.add(session);
            try {
                getRoom.byId(roomId,userId);
                matches.findActiveIdByRoomId(roomId).ifPresent(id -> gameplay.reconnect(id,userId));
            } catch (RuntimeException denied) {
                sessions.remove(session);
                closeQuietly(session,CloseStatus.POLICY_VIOLATION);
                return;
            }
        }
        if(!session.isOpen())return;
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
                case "ROOM_STATE" -> send(session, RoomStateMessage.of("ROOM_STATE", getRoom.byId(roomId, userId)));
                case "RECONNECT" -> reconnect(session, userId, requiredUuid(envelope, "matchId"));
                case "READY" -> {
                    membership.ready(roomId, userId, envelope.path("ready").asBoolean(false));

                }
                case "START_MATCH" -> {
                    GameMatchResult started = startMatch.execute(roomId, userId);

                }
                case "ANSWER" -> submitMatchAnswer(envelope, roomId, userId);
                case "LEAVE" -> {
                    var active=matches.findActiveIdByRoomId(roomId);
                    if(active.isPresent())gameplay.leave(active.get(),userId);
                    else membership.leave(roomId, userId);
                    if(active.isEmpty())broadcast(roomId, Map.of("type", "MEMBER_LEFT", "userId", userId.toString()));
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
        writers.remove(session.getId());
        lastPong.remove(session.getId());
        Object roomValue = session.getAttributes().get(ROOM_ID_ATTRIBUTE);
        if (roomValue instanceof UUID roomId) {
            Set<WebSocketSession> members = sessionsByRoom.get(roomId);
            if (members != null) {
                synchronized(members) {
                    members.remove(session);
                    UUID userId=attribute(session,USER_ID_ATTRIBUTE);
                    if(members.stream().noneMatch(s -> s.isOpen() && userId.equals(attribute(s,USER_ID_ATTRIBUTE))))
                        matches.findActiveIdByRoomId(roomId).ifPresent(id -> gameplay.disconnected(id,userId));
                }
            }
        }
    }

    private void reconnect(WebSocketSession session, UUID userId, UUID matchId) throws IOException {
        var state=getMatch.execute(matchId,userId);
        if(!state.roomId().equals(attribute(session,ROOM_ID_ATTRIBUTE)))throw new IllegalArgumentException("Match belongs to another room");
        gameplay.reconnect(matchId,userId);
        send(session, MatchStateMessage.of("MATCH_STATE", getMatch.execute(matchId, userId)));
        send(session, Map.of("type","RANKING_UPDATED","ranking",ranking.execute(matchId,userId)));
    }

    private void submitMatchAnswer(JsonNode envelope, UUID roomId, UUID userId) throws IOException {
        UUID matchId = requiredUuid(envelope, "matchId");
        GameMatchResult ownState = getMatch.execute(matchId, userId);
        if (!roomId.equals(ownState.roomId())) throw new IllegalArgumentException("Match belongs to another room");
        if (ownState.sessionId()==null) throw new IllegalArgumentException("Use the Wordly guesses endpoint");
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
        broadcast(ownState.roomId(), AnswerAcceptedMessage.of(userId.toString(), result));
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
                send(member, MatchStateMessage.of(type, getMatch.execute(matchId, userId)));
            } catch (RuntimeException ignored) {
                // A connected room member who is not a match player must not receive match data.
            }
        }
    }

    private void broadcastRoomState(UUID roomId, UUID userId) throws IOException {
        var room = getRoom.byId(roomId, userId);
        broadcast(roomId, RoomStateMessage.of("ROOM_STATE", room));
    }

    private void broadcast(UUID roomId, Object value) throws IOException {
        String payload = objectMapper.writeValueAsString(value);
        Set<WebSocketSession> members = sessionsByRoom.getOrDefault(roomId, Set.of());
        for (WebSocketSession member : members) {
            try { sendText(member,payload); }
            catch (IOException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Game socket delivery failed for {}",roomId); }
        }
    }

    private void send(WebSocketSession session, Object value) throws IOException {
        sendText(session,objectMapper.writeValueAsString(value));
    }

    private void sendText(WebSocketSession session,String payload) throws IOException {
        if(!session.isOpen())return;
        try {
            writer(session).sendMessage(new TextMessage(payload));
        } catch(IOException | org.springframework.web.socket.handler.SessionLimitExceededException failed) {
            closeQuietly(session,CloseStatus.SESSION_NOT_RELIABLE);
        }
    }

    private org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator writer(WebSocketSession session) {
        return writers.computeIfAbsent(session.getId(),ignored ->
            new org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator(session,10000,65536));
    }

    private void closeQuietly(WebSocketSession session,CloseStatus status) {
        try { if(session.isOpen())session.close(status); }
        catch(IOException | RuntimeException failed) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Could not close game socket {}",session.getId()); }
        finally {
            writers.remove(session.getId());
            lastPong.remove(session.getId());
        }
    }

    @Override
    protected void handlePongMessage(WebSocketSession session,PongMessage message) {
        lastPong.put(session.getId(),System.currentTimeMillis());
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay=10000)
    public void heartbeat() {
        long now=System.currentTimeMillis();
        for(var sessions:sessionsByRoom.values())for(var session:sessions) {
            if(!session.isOpen())continue;
            if(now-lastPong.getOrDefault(session.getId(),now)>=30000) {
                closeQuietly(session,CloseStatus.SESSION_NOT_RELIABLE);
                continue;
            }
            try { writer(session).sendMessage(new PingMessage()); }
            catch(IOException | RuntimeException failed) { closeQuietly(session,CloseStatus.SESSION_NOT_RELIABLE); }
        }
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
