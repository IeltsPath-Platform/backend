package com.ieltspath.game.application.result;

import com.ieltspath.game.application.result.RocketGameplayResults.Option;
import com.ieltspath.game.domain.vo.RocketEndReason;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * What every player of a Rocket match is told as rounds move on. Published inside the transaction that made the change
 * and delivered after it commits, so a client never hears of a round the database does not have yet.
 */
public final class RocketRoundEvents {
    private RocketRoundEvents() {}

    /** A round opened. It never carries the answer. */
    public record Started(UUID roomId, UUID matchId, UUID roundId, int number, int total, String meaning,
            List<Option> options, Instant opensAt, Instant deadlineAt, Instant serverTime) {
        public Started {
            options = List.copyOf(options);
        }

        public static Started of(UUID roomId, UUID matchId, int total, com.ieltspath.game.domain.aggregate.RocketRound round,
                Instant serverTime) {
            return new Started(roomId, matchId, round.id(), round.sequence(), total, round.meaning(),
                    round.options().stream().map(option -> new Option(option.optionId(), option.word())).toList(),
                    round.opensAt(), round.deadlineAt(), serverTime);
        }
    }

    /** Someone answered an open round; what they chose stays private until the round closes. */
    public record Answered(UUID roomId, UUID matchId, UUID roundId, UUID userId, int answeredCount, int aliveCount) {}

    /** A round closed: the answer, every expected player's outcome, and what comes next. */
    public record Closed(UUID roomId, UUID matchId, UUID roundId, UUID correctOptionId, List<PlayerOutcome> answers,
            Instant nextRoundAt, boolean matchFinished, RocketEndReason endReason) {
        public Closed {
            answers = List.copyOf(answers);
        }
    }

    /** Standings changed outside a round close: someone left, dropped their connection or came back. */
    public record RankingChanged(UUID roomId, UUID matchId) {}

    /** The match ended between rounds, e.g. when the last opponent left during a reveal pause. */
    public record Finished(UUID roomId, UUID matchId, RocketEndReason endReason) {}

    /** {@code optionId} is null for a player who did not answer in time. */
    public record PlayerOutcome(UUID userId, UUID optionId, boolean correct, int points, int livesRemaining,
            long totalScore, String status) {}
}
