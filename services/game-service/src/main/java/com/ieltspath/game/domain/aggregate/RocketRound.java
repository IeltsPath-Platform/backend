package com.ieltspath.game.domain.aggregate;

import com.ieltspath.game.domain.exception.SoloGameplayException;
import com.ieltspath.game.domain.vo.RocketOption;
import com.ieltspath.game.domain.vo.RocketRules;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One Rocket question shared by every player of a match. It opens, takes at most one answer per player until its
 * deadline, and closes exactly once. Answers are scored on arrival but stay private until the round closes, so players
 * cannot copy each other.
 */
public class RocketRound {
    public enum Status { PENDING, OPEN, CLOSED }

    public enum CloseReason { ALL_ANSWERED, DEADLINE, MATCH_ENDED }

    /** {@code optionId} is null when the player gave no answer before the round closed. */
    public record Answer(UUID playerId, UUID optionId, boolean correct, int points, Instant receivedAt,
            long responseMs, String requestId) {
        public boolean answered() { return optionId != null; }
    }

    private final UUID id;
    private final UUID matchId;
    private final int sequence;
    private final UUID sourceWordId;
    private final String meaning;
    private final List<RocketOption> options;
    private final UUID correctOptionId;
    private final Map<UUID, Answer> answers = new LinkedHashMap<>();
    private Status status = Status.PENDING;
    private Instant opensAt;
    private Instant deadlineAt;
    private Duration limit;
    private Instant closedAt;
    private CloseReason closeReason;

    public RocketRound(UUID id, UUID matchId, int sequence, UUID sourceWordId, String meaning, List<RocketOption> options,
            UUID correctOptionId) {
        if (sequence < 1) throw new IllegalArgumentException("Sequence starts at 1");
        if (meaning == null || meaning.isBlank()) throw new IllegalArgumentException("A round needs a meaning");
        if (options == null || options.size() != 3) throw new IllegalArgumentException("A round has exactly three options");
        if (options.stream().map(RocketOption::optionId).distinct().count() != 3)
            throw new IllegalArgumentException("Option ids must be unique");
        if (options.stream().noneMatch(option -> option.optionId().equals(correctOptionId)))
            throw new IllegalArgumentException("The correct option must be one of the options");
        this.id = Objects.requireNonNull(id);
        this.matchId = Objects.requireNonNull(matchId);
        this.sequence = sequence;
        this.sourceWordId = Objects.requireNonNull(sourceWordId);
        this.meaning = meaning;
        this.options = List.copyOf(options);
        this.correctOptionId = correctOptionId;
    }

    public static RocketRound reconstitute(UUID id, UUID matchId, int sequence, UUID sourceWordId, String meaning,
            List<RocketOption> options, UUID correctOptionId, Status status, Instant opensAt, Instant deadlineAt,
            Instant closedAt, CloseReason closeReason, Collection<Answer> answers) {
        RocketRound round = new RocketRound(id, matchId, sequence, sourceWordId, meaning, options, correctOptionId);
        round.status = status;
        round.opensAt = opensAt;
        round.deadlineAt = deadlineAt;
        round.limit = opensAt == null || deadlineAt == null ? null : Duration.between(opensAt, deadlineAt);
        round.closedAt = closedAt;
        round.closeReason = closeReason;
        answers.forEach(answer -> round.answers.put(answer.playerId(), answer));
        return round;
    }

    /** Sets when a pending round opens, e.g. after the previous round's reveal pause. */
    public void schedule(Instant opensAt) {
        if (status != Status.PENDING) throw new IllegalStateException("Only a pending round can be scheduled");
        this.opensAt = Objects.requireNonNull(opensAt);
    }

    public boolean dueToOpen(Instant now) {
        return status == Status.PENDING && opensAt != null && !now.isBefore(opensAt);
    }

    public void open(Instant at, Duration timeLimit) {
        if (status != Status.PENDING) throw new IllegalStateException("Only a pending round can open");
        if (timeLimit == null || timeLimit.isNegative() || timeLimit.isZero())
            throw new IllegalArgumentException("Time limit must be positive");
        status = Status.OPEN;
        opensAt = at;
        limit = timeLimit;
        deadlineAt = at.plus(timeLimit);
    }

    /**
     * Records a player's answer as received by the server at {@code receivedAt}. Repeating the same request returns the
     * stored answer; anything else after the first answer, after the deadline or for another option set is rejected.
     */
    public Answer accept(UUID playerId, UUID optionId, String requestId, Instant receivedAt) {
        Objects.requireNonNull(playerId, "Player is required");
        if (requestId == null || requestId.isBlank()) throw new IllegalArgumentException("Request id is required");
        Answer existing = answers.get(playerId);
        if (existing != null && requestId.equals(existing.requestId())) {
            if (!Objects.equals(existing.optionId(), optionId))
                throw new SoloGameplayException(409, "REQUEST_ID_CONFLICT", "Request id was used for another answer");
            return existing;
        }
        if (status != Status.OPEN || !receivedAt.isBefore(deadlineAt))
            throw new SoloGameplayException(409, "ROUND_CLOSED", "This round is closed");
        if (existing != null) throw new SoloGameplayException(409, "ALREADY_ANSWERED", "You already answered this round");
        if (options.stream().noneMatch(option -> option.optionId().equals(optionId)))
            throw new SoloGameplayException(400, "INVALID_OPTION", "Unknown option for this round");
        boolean correct = correctOptionId.equals(optionId);
        Answer answer = new Answer(playerId, optionId, correct, RocketRules.points(correct, receivedAt, deadlineAt, limit),
                receivedAt, Math.max(0, Duration.between(opensAt, receivedAt).toMillis()), requestId);
        answers.put(playerId, answer);
        return answer;
    }

    /** True once every player still in the round has answered; the round can then close early. */
    public boolean answeredBy(Collection<UUID> players) {
        return !players.isEmpty() && players.stream().allMatch(answers::containsKey);
    }

    /**
     * Closes the round once and returns every expected player's result; players who did not answer get a miss.
     * Closing again is rejected so a deadline worker and a last answer cannot both settle the same round.
     */
    public Map<UUID, Answer> close(Instant at, CloseReason reason, Collection<UUID> expectedPlayers) {
        if (status != Status.OPEN) throw new IllegalStateException("Only an open round can close");
        status = Status.CLOSED;
        closedAt = at;
        closeReason = Objects.requireNonNull(reason);
        for (UUID player : expectedPlayers)
            answers.putIfAbsent(player, new Answer(player, null, false, 0, null, 0, null));
        return Map.copyOf(answers);
    }

    public boolean expired(Instant now) {
        return status == Status.OPEN && !now.isBefore(deadlineAt);
    }

    /** The answer is revealed only after the round closes. */
    public Optional<UUID> revealedCorrectOptionId() {
        return status == Status.CLOSED ? Optional.of(correctOptionId) : Optional.empty();
    }

    public UUID id() { return id; }
    public UUID matchId() { return matchId; }
    public int sequence() { return sequence; }
    public UUID sourceWordId() { return sourceWordId; }
    public String meaning() { return meaning; }
    public List<RocketOption> options() { return options; }
    /** For persistence only; API code must use {@link #revealedCorrectOptionId()}. */
    public UUID correctOptionId() { return correctOptionId; }
    public Status status() { return status; }
    public Instant opensAt() { return opensAt; }
    public Instant deadlineAt() { return deadlineAt; }
    public Instant closedAt() { return closedAt; }
    public CloseReason closeReason() { return closeReason; }
    public Map<UUID, Answer> answers() { return Map.copyOf(answers); }
    public Optional<Answer> answerOf(UUID playerId) { return Optional.ofNullable(answers.get(playerId)); }
}
