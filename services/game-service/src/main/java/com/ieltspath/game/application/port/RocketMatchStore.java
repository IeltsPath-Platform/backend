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

    /** Matches with an expired open round or a round due to open, for the round worker. */
    List<UUID> dueMatches(Instant now, int limit);

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
