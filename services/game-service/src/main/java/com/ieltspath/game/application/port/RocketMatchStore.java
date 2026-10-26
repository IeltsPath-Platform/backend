package com.ieltspath.game.application.port;

import com.ieltspath.game.domain.vo.RocketEndReason;
import com.ieltspath.game.domain.vo.RocketPlayerProgress;
import com.ieltspath.game.domain.vo.RocketRoomSettings;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Rocket match and player rows. Locking order everywhere: match, then its players, then its rounds. */
public interface RocketMatchStore {
    /** Database time, read after taking locks, so answer timing does not depend on the app server's clock. */
    Instant now();

    /** Locks the match and its players until the transaction ends. */
    Optional<State> lock(UUID matchId);

    Optional<State> find(UUID matchId);

    void savePlayers(List<Player> players);

    /** Completes the match and returns its room to WAITING with readiness cleared. */
    void finish(UUID matchId, UUID roomId, RocketEndReason reason, Instant at);

    /** Completes the match and closes its room for good, as when a solo player leaves. */
    void finishAndCloseRoom(UUID matchId, UUID roomId, RocketEndReason reason, Instant at);

    /**
     * Matches with an expired open round, a round due to open or a player whose reconnect deadline passed, for the
     * round worker.
     */
    List<UUID> dueMatches(Instant now, int limit);

    /** Marks the member as gone from the room, so they can join another game. */
    void leaveRoom(UUID roomId, UUID userId, Instant at);

    /** The host's display name (or id) when the host is no longer a member of the room; empty while they are. */
    Optional<String> departedHostName(UUID roomId);

    /** Closes the room for good and marks every member as gone. */
    void closeRoom(UUID roomId, Instant at);

    /** The name the user has in the room, or their id when unknown. */
    String displayName(UUID roomId, UUID userId);

    /** ACTIVE to DISCONNECTED until {@code deadline}; false when the player was not ACTIVE. */
    boolean disconnect(UUID matchId, UUID userId, Instant at, Instant deadline);

    /** DISCONNECTED back to ACTIVE before the deadline; false otherwise. */
    boolean reconnect(UUID matchId, UUID userId, Instant at);

    /** Users of the match still DISCONNECTED after their reconnect deadline. */
    List<UUID> expiredDisconnects(UUID matchId, Instant at);

    record State(UUID matchId, UUID roomId, RocketRoomSettings settings, String status, RocketEndReason endReason,
            Instant startedAt, Instant endedAt, List<Player> players) {
        public State {
            players = List.copyOf(players);
        }

        public boolean finished() { return !"IN_PROGRESS".equals(status); }

        public Optional<Player> player(UUID userId) {
            return players.stream().filter(player -> player.userId().equals(userId)).findFirst();
        }

        public State with(List<Player> updated, RocketEndReason reason, Instant endedAt) {
            return new State(matchId, roomId, settings, reason == null ? status : "COMPLETED", reason, startedAt,
                    reason == null ? this.endedAt : endedAt, updated);
        }
    }

    /** {@code status} uses GameMatchPlayer statuses: ACTIVE, DISCONNECTED, FINISHED, ELIMINATED, FORFEITED. */
    record Player(UUID id, UUID userId, String status, RocketPlayerProgress progress, Instant finishedAt) {
        public boolean playing() { return "ACTIVE".equals(status) || "DISCONNECTED".equals(status); }

        public boolean forfeited() { return "FORFEITED".equals(status); }

        public Player with(String nextStatus, RocketPlayerProgress nextProgress, Instant at) {
            return new Player(id, userId, nextStatus, nextProgress, playing() && !nextStatus.equals(status) ? at : finishedAt);
        }
    }
}
