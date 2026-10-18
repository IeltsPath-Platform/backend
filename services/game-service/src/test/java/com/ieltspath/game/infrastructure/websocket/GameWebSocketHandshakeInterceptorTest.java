package com.ieltspath.game.infrastructure.websocket;

import com.ieltspath.game.application.port.WebSocketTicketStore;
import com.ieltspath.game.domain.aggregate.GameRoom;
import com.ieltspath.game.domain.repository.GameRoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameWebSocketHandshakeInterceptorTest {
    private final WebSocketTicketStore tickets = mock(WebSocketTicketStore.class);
    private final GameRoomRepository rooms = mock(GameRoomRepository.class);
    private final ServerHttpRequest request = mock(ServerHttpRequest.class);
    private final ServerHttpResponse response = mock(ServerHttpResponse.class);
    private final WebSocketHandler handler = mock(WebSocketHandler.class);
    private final GameWebSocketHandshakeInterceptor interceptor = new GameWebSocketHandshakeInterceptor(tickets, rooms);

    @Test
    void acceptsCommaSeparatedProtocolsAndConsumesTicketOnce() {
        var headers = new HttpHeaders();
        headers.add("Sec-WebSocket-Protocol", " game.v1, ticket.opaque ");
        when(request.getHeaders()).thenReturn(headers);
        var user = UUID.randomUUID();
        var room = new GameRoom(UUID.randomUUID(), "AB7K2Q", user, 4, Instant.EPOCH);
        when(tickets.consume("opaque")).thenReturn(Optional.of(new WebSocketTicketStore.Ticket(
                "opaque", room.id(), user, Instant.now().plusSeconds(30))), Optional.empty());
        when(rooms.findById(room.id())).thenReturn(Optional.of(room));
        var attributes = new HashMap<String, Object>();
        assertTrue(interceptor.beforeHandshake(request, response, handler, attributes));
        assertEquals(user, attributes.get(GameWebSocketHandler.USER_ID_ATTRIBUTE));
        assertEquals(room.id(), attributes.get(GameWebSocketHandler.ROOM_ID_ATTRIBUTE));
        assertFalse(interceptor.beforeHandshake(request, response, handler, new HashMap<>()));
    }

    @Test
    void rejectsTicketForRemovedMemberEvenWithValidProtocols() {
        var headers = new HttpHeaders();
        headers.add("Sec-WebSocket-Protocol", "game.v1");
        headers.add("Sec-WebSocket-Protocol", "other, ticket.opaque");
        when(request.getHeaders()).thenReturn(headers);
        var host = UUID.randomUUID();
        var guest = UUID.randomUUID();
        var room = new GameRoom(UUID.randomUUID(), "AB7K2Q", host, 4, Instant.EPOCH);
        room.join(guest, Instant.EPOCH);
        room.kick(host, guest, Instant.EPOCH);
        when(tickets.consume("opaque")).thenReturn(Optional.of(new WebSocketTicketStore.Ticket(
                "opaque", room.id(), guest, Instant.now().plusSeconds(30))));
        when(rooms.findById(room.id())).thenReturn(Optional.of(room));
        var attributes = new HashMap<String, Object>();
        assertFalse(interceptor.beforeHandshake(request, response, handler, attributes));
        assertTrue(attributes.isEmpty());
    }

    @Test
    void closedRoomRejectsUnconsumedTicketForStillActiveHost() {
        var headers = new HttpHeaders();
        headers.add("Sec-WebSocket-Protocol", "game.v1, ticket.opaque");
        when(request.getHeaders()).thenReturn(headers);
        var host = UUID.randomUUID();
        var room = new GameRoom(UUID.randomUUID(), "AB7K2Q", host, 4, Instant.EPOCH);
        room.close(host, Instant.EPOCH);
        when(tickets.consume("opaque")).thenReturn(Optional.of(new WebSocketTicketStore.Ticket(
                "opaque", room.id(), host, Instant.now().plusSeconds(30))));
        when(rooms.findById(room.id())).thenReturn(Optional.of(room));
        assertFalse(interceptor.beforeHandshake(request, response, handler, new HashMap<>()));
    }

    @Test
    void missingProtocolOrTicketNeverConsumesAGrant() {
        var headers = new HttpHeaders();
        headers.add("Sec-WebSocket-Protocol", "game.v1, other");
        when(request.getHeaders()).thenReturn(headers);
        assertFalse(interceptor.beforeHandshake(request, response, handler, new HashMap<>()));
        headers.set("Sec-WebSocket-Protocol", "ticket.opaque");
        assertFalse(interceptor.beforeHandshake(request, response, handler, new HashMap<>()));
        verifyNoInteractions(tickets, rooms);
    }
}
