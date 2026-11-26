package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.port.WebSocketTicketStore;
import com.ieltspath.game.application.port.WebSocketTicketStore.Ticket;
import com.ieltspath.game.application.result.GameTicketResult;
import com.ieltspath.game.domain.exception.GameRoomNotFoundException;
import com.ieltspath.game.domain.repository.GameRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;

@Service
public class CreateWebSocketTicketUseCase {
    private final GameRoomRepository roomRepository;
    private final WebSocketTicketStore ticketStore;

    public CreateWebSocketTicketUseCase(GameRoomRepository roomRepository, WebSocketTicketStore ticketStore) {
        this.roomRepository = roomRepository;
        this.ticketStore = ticketStore;
    }

    @Transactional(readOnly = true)
    public GameTicketResult execute(UUID roomId, UUID userId, Set<String> roles) {
        if (roles == null || !roles.contains("CUSTOMER")) {
            throw new AccessDeniedException("CUSTOMER role is required for game WebSocket access");
        }
        var room = roomRepository.findById(roomId).orElseThrow(() -> new GameRoomNotFoundException(roomId));
        if (!room.hasActiveMember(userId)) throw new GameRoomNotFoundException(roomId);
        Ticket ticket = ticketStore.issue(roomId, userId, roles);
        return new GameTicketResult(ticket.token(), ticket.expiresAt(),
                List.of("game.v1", "ticket." + ticket.token()));
    }
}
