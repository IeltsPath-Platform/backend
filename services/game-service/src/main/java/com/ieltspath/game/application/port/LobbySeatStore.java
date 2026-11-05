package com.ieltspath.game.application.port;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Seats of multiplayer lobby members whose last socket closed; the room lock is taken by the caller. */
public interface LobbySeatStore {
    /** Locks the room row for this transaction; false when there is no such room. */
    boolean lockRoom(UUID roomId);

    /** JOINED or READY to DISCONNECTED until {@code deadline}, in a waiting multiplayer room; false otherwise. */
    boolean disconnect(UUID roomId, UUID userId, Instant deadline);

    /** DISCONNECTED back to JOINED before the deadline; false otherwise. */
    boolean reconnect(UUID roomId, UUID userId, Instant now);

    /** True while the seat is held: DISCONNECTED with the deadline still ahead. */
    boolean held(UUID roomId, UUID userId, Instant now);

    boolean expired(UUID roomId, UUID userId, Instant now);

    /** Expired seats in waiting rooms, oldest deadline first. */
    List<Seat> due(Instant now, int limit);

    /** Waiting rooms where this user's seat has expired. */
    List<UUID> dueRoomsOf(UUID userId, Instant now);

    record Seat(UUID roomId, UUID userId) {}
}
