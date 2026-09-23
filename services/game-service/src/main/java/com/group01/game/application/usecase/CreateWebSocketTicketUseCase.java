package com.group01.game.application.usecase;

import com.group01.game.application.port.WebSocketTicketStore;
import com.group01.game.application.port.WebSocketTicketStore.Ticket;
import com.group01.game.domain.exception.GameRoomNotFoundException;
import com.group01.game.domain.repository.GameRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CreateWebSocketTicketUseCase {
    private final GameRoomRepository roomRepository;
    private final WebSocketTicketStore ticketStore;

    public CreateWebSocketTicketUseCase(GameRoomRepository roomRepository, WebSocketTicketStore ticketStore) {
        this.roomRepository = roomRepository;
        this.ticketStore = ticketStore;
    }

    @Transactional(readOnly = true)
    public TicketResult execute(UUID roomId, UUID userId) {
        var room = roomRepository.findById(roomId).orElseThrow(() -> new GameRoomNotFoundException(roomId));
        if (!room.hasActiveMember(userId)) throw new GameRoomNotFoundException(roomId);
        Ticket ticket = ticketStore.issue(roomId, userId);
        return new TicketResult(ticket.token(), ticket.expiresAt(),
                List.of("game.v1", "ticket." + ticket.token()));
    }

    public record TicketResult(String ticket, java.time.Instant expiresAt, List<String> subprotocols) {
        public TicketResult { subprotocols = List.copyOf(subprotocols); }
    }
}
