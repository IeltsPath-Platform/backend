package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.port.LobbySeatStore;
import com.ieltspath.game.application.result.GameRoomResult;
import com.ieltspath.game.application.result.RoomClosedEvent;
import com.ieltspath.game.application.result.RoomUpdatedEvent;
import com.ieltspath.game.domain.aggregate.GameRoom;
import com.ieltspath.game.domain.repository.GameRoomRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A multiplayer lobby member keeps the seat for {@link #GRACE} after the last socket closes. Reconnecting in time keeps
 * the seat, not ready; otherwise the member leaves the room as if they had pressed leave, closing it when they host.
 */
@Service
public class LobbySeatUseCase {
    public static final Duration GRACE = Duration.ofSeconds(30);

    private final LobbySeatStore seats;
    private final GameRoomRepository rooms;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public LobbySeatUseCase(LobbySeatStore seats, GameRoomRepository rooms, ApplicationEventPublisher events, Clock clock) {
        this.seats = seats;
        this.rooms = rooms;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Takes the room lock first, so a room save in flight cannot undo the change. The lock is taken without loading the
     * room, so the room read for the broadcast afterwards shows the new seat.
     */
    @Transactional
    public void disconnected(UUID roomId, UUID userId) {
        if (!seats.lockRoom(roomId)) return;
        if (seats.disconnect(roomId, userId, clock.instant().plus(GRACE))) publish(roomId);
    }

    /** Called on every heartbeat, so the room is locked only when a seat is actually held. */
    @Transactional
    public void reconnected(UUID roomId, UUID userId) {
        if (!seats.held(roomId, userId, clock.instant()) || !seats.lockRoom(roomId)) return;
        if (seats.reconnect(roomId, userId, clock.instant())) publish(roomId);
    }

    public List<LobbySeatStore.Seat> due(int limit) {
        return seats.due(clock.instant(), limit);
    }

    /** Leaves the room for an expired seat. Safe to repeat and to race: the room lock and a fresh check decide. */
    @Transactional
    public void expire(UUID roomId, UUID userId) {
        var room = rooms.findForUpdate(roomId).orElse(null);
        if (room == null || room.status() != GameRoom.GameRoomStatus.WAITING
                || !seats.expired(roomId, userId, clock.instant())) return;
        room.leave(userId, clock.instant());
        var saved = rooms.save(room);
        if (saved.status() == GameRoom.GameRoomStatus.CLOSED) {
            String name = saved.members().get(saved.hostUserId()).displayName();
            events.publishEvent(new RoomClosedEvent(roomId,
                    "Chủ phòng " + (name == null ? saved.hostUserId().toString() : name) + " đã đóng phòng"));
        } else events.publishEvent(new RoomUpdatedEvent(GameRoomResult.from(saved)));
    }

    /**
     * Applies the expiry to this user's own seats now, so a check does not wait for the worker. It commits on its own,
     * so the caller's next read already sees the member gone.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void expireFor(UUID userId) {
        for (UUID roomId : seats.dueRoomsOf(userId, clock.instant())) expire(roomId, userId);
    }

    private void publish(UUID roomId) {
        rooms.findById(roomId).filter(room -> room.status() == GameRoom.GameRoomStatus.WAITING)
                .ifPresent(room -> events.publishEvent(new RoomUpdatedEvent(GameRoomResult.from(room))));
    }
}
