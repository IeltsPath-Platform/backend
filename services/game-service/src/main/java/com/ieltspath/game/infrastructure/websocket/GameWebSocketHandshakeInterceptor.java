package com.ieltspath.game.infrastructure.websocket;

import com.ieltspath.game.application.port.WebSocketTicketStore;
import com.ieltspath.game.domain.repository.GameRoomRepository;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class GameWebSocketHandshakeInterceptor implements HandshakeInterceptor {
    private static final String TICKET_PREFIX = "ticket.";
    private final WebSocketTicketStore ticketStore;
    private final GameRoomRepository roomRepository;

    public GameWebSocketHandshakeInterceptor(WebSocketTicketStore ticketStore, GameRoomRepository roomRepository) {
        this.ticketStore = ticketStore;
        this.roomRepository = roomRepository;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        var headerValues = request.getHeaders().get("Sec-WebSocket-Protocol");
        if (headerValues == null) return false;
        var protocols = headerValues.stream().flatMap(value -> java.util.Arrays.stream(value.split(",")))
                .map(String::trim).filter(value -> !value.isEmpty()).toList();
        if (!protocols.contains("game.v1")) return false;
        String opaqueTicket = protocols.stream().filter(value -> value.startsWith(TICKET_PREFIX))
                .map(value -> value.substring(TICKET_PREFIX.length())).findFirst().orElse(null);
        if (opaqueTicket == null || opaqueTicket.isBlank()) return false;
        var ticket = ticketStore.consume(opaqueTicket);
        if (ticket.isEmpty()) return false;
        var grant = ticket.get();
        var room = roomRepository.findById(grant.roomId());
        if (room.isEmpty() || !room.get().hasActiveMember(grant.userId())) return false;
        if (room.get().status() == com.ieltspath.game.domain.aggregate.GameRoom.GameRoomStatus.CLOSED
                || room.get().status() == com.ieltspath.game.domain.aggregate.GameRoom.GameRoomStatus.EXPIRED) return false;
        attributes.put(GameWebSocketHandler.ROOM_ID_ATTRIBUTE, grant.roomId());
        attributes.put(GameWebSocketHandler.USER_ID_ATTRIBUTE, grant.userId());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}
}
