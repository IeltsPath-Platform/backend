package com.ieltspath.game.application.result;

import com.ieltspath.game.domain.vo.RocketEndReason;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Response shapes of the Rocket round API; they never carry the correct option of a round that is still open. */
public final class RocketGameplayResults {
    private RocketGameplayResults() {}

    public record Settings(int questionCount, int initialLives, int secondsPerQuestion) {}

    public record Option(UUID optionId, String word) {}

    public record PlayerSummary(int livesRemaining, long totalScore, String status) {}

    public record Me(boolean answered, UUID optionId) {}

    /** {@code deadlineAt} is null until the round opens; {@code opensAt} is the scheduled time of a pending round. */
    public record Round(UUID roundId, int number, int total, String meaning, List<Option> options, Instant opensAt,
            Instant deadlineAt, Instant serverTime, Me me, PlayerSummary player) {}

    /** The caller's view of a closed round. {@code chosenOptionId} is null when they did not answer in time. */
    public record RoundResult(UUID roundId, UUID correctOptionId, UUID chosenOptionId, boolean correct, int points,
            int livesRemaining, long totalScore, String status, Instant nextRoundAt, boolean matchFinished,
            RocketEndReason endReason) {}

    public record Current(UUID matchId, Settings settings, Round round, RoundResult lastRoundResult,
            boolean matchFinished, Instant serverTime) {}

    /** {@code roundResult} is present when this answer closed the round, which is always the case in solo play. */
    public record AnswerAck(String requestId, UUID roundId, UUID optionId, Instant receivedAt, RoundResult roundResult) {}
}
