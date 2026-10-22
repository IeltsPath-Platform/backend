package com.ieltspath.game.domain.vo;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Scoring, lives and match-ending rules shared by solo and multiplayer Rocket. */
public final class RocketRules {
    public static final int BASE_POINTS = 100;
    public static final int MAX_SPEED_POINTS = 100;

    private RocketRules() {}

    /**
     * A correct answer earns 100 plus up to 100 for speed: {@code floor(100 × time left / time limit)}, using the time
     * the server received the answer. Wrong answers and timeouts earn nothing.
     */
    public static int points(boolean correct, Instant receivedAt, Instant deadline, Duration limit) {
        if (!correct) return 0;
        long left = Math.max(0, Duration.between(receivedAt, deadline).toMillis());
        long total = Math.max(1, limit.toMillis());
        return BASE_POINTS + (int) Math.min(MAX_SPEED_POINTS, MAX_SPEED_POINTS * left / total);
    }

    /** One finished round for one player: a miss (wrong or no answer) costs exactly one life. */
    public static RocketPlayerProgress apply(RocketPlayerProgress before, boolean answered, boolean correct, int points,
            long responseMs) {
        if (!before.alive()) throw new IllegalStateException("Eliminated players take no further rounds");
        if (correct) {
            return new RocketPlayerProgress(before.livesRemaining(), before.totalScore() + points,
                    before.roundsPlayed() + 1, before.correctCount() + 1, before.correctTimeMs() + responseMs);
        }
        return new RocketPlayerProgress(before.livesRemaining() - 1, before.totalScore(), before.roundsPlayed() + 1,
                before.correctCount(), before.correctTimeMs());
    }

    /**
     * Whether the match ends after a round closes. {@code contenders} are players who have not left; {@code
     * startedPlayers} is how many began. Running out of lives is checked before the last round, so a solo player who
     * misses the final question with their last life gets NO_LIVES_LEFT.
     */
    public static Optional<RocketEndReason> endReason(boolean solo, int startedPlayers, List<RocketPlayerProgress> contenders,
            int closedRounds, int totalRounds) {
        long alive = contenders.stream().filter(RocketPlayerProgress::alive).count();
        if (solo) {
            if (contenders.isEmpty()) return Optional.of(RocketEndReason.PLAYER_LEFT);
            if (alive == 0) return Optional.of(RocketEndReason.NO_LIVES_LEFT);
        } else {
            if (contenders.isEmpty()) return Optional.of(RocketEndReason.ALL_PLAYERS_LEFT);
            if (alive == 0) return Optional.of(RocketEndReason.ALL_ELIMINATED);
            if (alive == 1 && startedPlayers > 1) return Optional.of(RocketEndReason.LAST_PLAYER_STANDING);
        }
        return closedRounds >= totalRounds ? Optional.of(RocketEndReason.ALL_ROUNDS_COMPLETED) : Optional.empty();
    }
}
