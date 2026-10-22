package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.port.RocketMatchStore;
import com.ieltspath.game.application.port.RocketMatchStore.Player;
import com.ieltspath.game.application.port.RocketMatchStore.State;
import com.ieltspath.game.application.result.RocketGameplayResults.AnswerAck;
import com.ieltspath.game.application.result.RocketGameplayResults.Current;
import com.ieltspath.game.application.result.RocketGameplayResults.Left;
import com.ieltspath.game.application.result.RocketGameplayResults.Result;
import com.ieltspath.game.application.result.RocketGameplayResults.Me;
import com.ieltspath.game.application.result.RocketGameplayResults.Option;
import com.ieltspath.game.application.result.RocketGameplayResults.PlayerSummary;
import com.ieltspath.game.application.result.RocketGameplayResults.Round;
import com.ieltspath.game.application.result.RocketGameplayResults.RoundResult;
import com.ieltspath.game.application.result.RocketGameplayResults.Settings;
import com.ieltspath.game.application.result.RocketRoundEvents;
import com.ieltspath.game.application.result.RoomClosedEvent;
import com.ieltspath.game.application.result.WordlyPlayerLeftEvent;
import com.ieltspath.game.domain.aggregate.RocketRound;
import com.ieltspath.game.domain.exception.GameMatchNotFoundException;
import com.ieltspath.game.domain.exception.SoloGameplayException;
import com.ieltspath.game.domain.repository.RocketRoundRepository;
import com.ieltspath.game.domain.vo.RocketEndReason;
import com.ieltspath.game.domain.vo.RocketPlayerProgress;
import com.ieltspath.game.domain.vo.RocketRules;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Rocket rounds as players see them. Every path first settles time-based changes ({@link #advance}): an open round
 * past its deadline closes, and the next round opens once its reveal pause is over. The worker calls the same method,
 * and the match lock makes a request and the worker settle a round only once between them. Each change is announced
 * as a {@link RocketRoundEvents} event from the same transaction.
 */
@Service
public class RocketGameplayUseCase {
    /** How long a multiplayer player who lost their connection keeps their place, as in Wordly. */
    public static final Duration RECONNECT_GRACE = Duration.ofSeconds(30);

    private final RocketMatchStore matches;
    private final RocketRoundRepository rounds;
    private final TransactionTemplate tx;
    private final ApplicationEventPublisher events;

    public RocketGameplayUseCase(RocketMatchStore matches, RocketRoundRepository rounds, PlatformTransactionManager manager,
            ApplicationEventPublisher events) {
        this.matches = matches;
        this.rounds = rounds;
        this.tx = new TransactionTemplate(manager);
        this.events = events;
    }

    /** Closes an expired round and opens a due one. Safe to repeat; does nothing for a finished match. */
    public void advance(UUID matchId) {
        tx.executeWithoutResult(status -> matches.lock(matchId)
                .ifPresent(state -> settle(state, rounds.findByMatch(matchId), matches.now())));
    }

    public Current current(UUID matchId, UUID userId) {
        advance(matchId);
        return tx.execute(status -> {
            State state = matches.find(matchId).orElseThrow(() -> new GameMatchNotFoundException(matchId));
            Player player = state.player(userId).orElseThrow(() -> new GameMatchNotFoundException(matchId));
            List<RocketRound> all = rounds.findByMatch(matchId);
            Instant now = matches.now();
            RocketRound shown = state.finished() ? null : all.stream()
                    .filter(round -> round.status() != RocketRound.Status.CLOSED).findFirst().orElse(null);
            RoundResult last = all.stream().filter(round -> round.status() == RocketRound.Status.CLOSED)
                    .max(Comparator.comparingInt(RocketRound::sequence))
                    .map(round -> roundResult(state, all, round, player)).orElse(null);
            return new Current(matchId, settings(state), shown == null ? null : view(state, shown, player, now), last,
                    state.finished(), now);
        });
    }

    /**
     * Answers the given round. The deadline is settled first, in its own transaction, so a late answer finds the round
     * already closed and gets ROUND_CLOSED without undoing that close.
     */
    public AnswerAck answer(UUID matchId, UUID roundId, UUID userId, String requestId, UUID optionId) {
        advance(matchId);
        return tx.execute(status -> {
            State state = matches.lock(matchId).orElseThrow(() -> new GameMatchNotFoundException(matchId));
            Player player = state.player(userId).orElseThrow(() -> new GameMatchNotFoundException(matchId));
            List<RocketRound> all = rounds.findByMatch(matchId);
            RocketRound round = all.stream().filter(candidate -> candidate.id().equals(roundId)).findFirst()
                    .orElseThrow(() -> new SoloGameplayException(404, "ROUND_NOT_FOUND", "Round not found"));
            boolean replay = round.answerOf(player.id()).map(RocketRound.Answer::requestId)
                    .filter(requestId::equals).isPresent();
            if (!replay && all.stream().anyMatch(other -> other != round && other.answerOf(player.id())
                    .map(RocketRound.Answer::requestId).filter(requestId::equals).isPresent()))
                throw new SoloGameplayException(409, "REQUEST_ID_CONFLICT", "Request id was used for another answer");
            if (!replay && state.finished()) throw new SoloGameplayException(409, "MATCH_FINISHED", "This match has finished");
            if (!replay && !player.playing())
                throw new SoloGameplayException(409, "PLAYER_NOT_ACTIVE", "You are no longer playing this match");
            Instant at = matches.now();
            RocketRound.Answer answer = round.accept(player.id(), optionId, requestId, at);
            State after = state;
            if (!replay) {
                Set<UUID> contenders = contenders(state);
                if (round.answeredBy(contenders)) after = closeRound(state, all, round, RocketRound.CloseReason.ALL_ANSWERED, at);
                else {
                    rounds.save(round);
                    int answered = (int) contenders.stream().filter(id -> round.answerOf(id).isPresent()).count();
                    events.publishEvent(new RocketRoundEvents.Answered(state.roomId(), matchId, roundId, userId, answered,
                            contenders.size()));
                }
            }
            Player updated = after.player(userId).orElse(player);
            RoundResult result = round.status() == RocketRound.Status.CLOSED ? roundResult(after, all, round, updated) : null;
            return new AnswerAck(requestId, roundId, optionId, answer.receivedAt(), result);
        });
    }

    /** Whether this match is a Rocket match, so shared match routes can hand it over. */
    public boolean isRocket(UUID matchId) {
        return matches.find(matchId).isPresent();
    }

    /**
     * Forfeits the caller. Points already won stay. A solo match then ends with PLAYER_LEFT, its open round closes
     * without costing a life and its room is closed. In multiplayer the others play on: the open round closes as soon
     * as everyone left in it has answered, and the match ends when nobody or one player is left standing. Leaving again,
     * or after the match ended, changes nothing.
     */
    public Left leave(UUID matchId, UUID userId) {
        advance(matchId);
        return tx.execute(status -> {
            State state = matches.lock(matchId).orElseThrow(() -> new GameMatchNotFoundException(matchId));
            Player player = state.player(userId).orElseThrow(() -> new GameMatchNotFoundException(matchId));
            if (!state.finished() && player.playing() && !state.settings().solo()) {
                state = depart(state, rounds.findByMatch(matchId), player, matches.now());
                player = state.player(userId).orElseThrow();
            } else if (!state.finished() && !state.settings().solo()) {
                matches.leaveRoom(state.roomId(), userId, matches.now());
            } else if (!state.finished() && player.playing()) {
                Instant at = matches.now();
                for (RocketRound round : rounds.findByMatch(matchId)) {
                    if (round.status() != RocketRound.Status.OPEN) continue;
                    round.close(at, RocketRound.CloseReason.MATCH_ENDED, Set.of());
                    rounds.save(round);
                }
                player = player.with("FORFEITED", player.progress(), at);
                matches.savePlayers(List.of(player));
                matches.finishAndCloseRoom(matchId, state.roomId(), RocketEndReason.PLAYER_LEFT, at);
                state = state.with(List.of(player), RocketEndReason.PLAYER_LEFT, at);
            }
            return new Left(matchId, state.status(), state.endReason(), player.status(), player.progress().totalScore(),
                    state.finished());
        });
    }

    /** Final numbers of a finished match; 409 MATCH_NOT_FINISHED while it runs. */
    public Result result(UUID matchId, UUID userId) {
        advance(matchId);
        return tx.execute(status -> {
            State state = matches.find(matchId).orElseThrow(() -> new GameMatchNotFoundException(matchId));
            Player player = state.player(userId).orElseThrow(() -> new GameMatchNotFoundException(matchId));
            if (!state.finished()) throw new SoloGameplayException(409, "MATCH_NOT_FINISHED", "This match is still running");
            var progress = player.progress();
            Instant ended = state.endedAt() == null ? state.startedAt() : state.endedAt();
            return new Result(matchId, state.endReason(), settings(state), state.settings().questionCount(),
                    progress.roundsPlayed(), progress.correctCount(), progress.livesRemaining(), progress.totalScore(),
                    player.status(), Duration.between(state.startedAt(), ended).toMillis(), state.startedAt(), ended);
        });
    }

    /** Called when the user's last socket to the room closes. */
    public void disconnected(UUID matchId, UUID userId) {
        tx.executeWithoutResult(status -> matches.lock(matchId).ifPresent(state -> {
            if (state.finished() || state.settings().solo()) return;
            Instant at = matches.now();
            if (matches.disconnect(matchId, userId, at, at.plus(RECONNECT_GRACE)))
                events.publishEvent(new RocketRoundEvents.RankingChanged(state.roomId(), matchId));
        }));
    }

    /** Called when the user connects to the room again; after the grace period they have already forfeited. */
    public void reconnected(UUID matchId, UUID userId) {
        advance(matchId);
        tx.executeWithoutResult(status -> matches.lock(matchId).ifPresent(state -> {
            if (state.finished() || state.settings().solo()) return;
            if (matches.reconnect(matchId, userId, matches.now()))
                events.publishEvent(new RocketRoundEvents.RankingChanged(state.roomId(), matchId));
        }));
    }

    /**
     * Forfeits a multiplayer player and settles what that changes: the match ends if nobody or one player is left
     * standing, otherwise an open round closes once everyone left in it has answered.
     */
    private State depart(State state, List<RocketRound> all, Player player, Instant at) {
        Player gone = player.with("FORFEITED", player.progress(), at);
        List<Player> players = state.players().stream().map(p -> p.id().equals(gone.id()) ? gone : p).toList();
        matches.savePlayers(List.of(gone));
        matches.leaveRoom(state.roomId(), gone.userId(), at);
        State after = state.with(players, null, null);
        events.publishEvent(new WordlyPlayerLeftEvent(state.roomId(), state.matchId(),
                gone.userId(), matches.displayName(state.roomId(), gone.userId())));
        Optional<RocketRound> open = all.stream().filter(round -> round.status() == RocketRound.Status.OPEN).findFirst();
        int closed = (int) all.stream().filter(round -> round.status() == RocketRound.Status.CLOSED).count();
        List<RocketPlayerProgress> standing = players.stream().filter(p -> !p.forfeited()).map(Player::progress).toList();
        Optional<RocketEndReason> end = RocketRules.endReason(false, players.size(), standing, closed,
                state.settings().questionCount());
        // Answers already given still count: if everyone left in the round has answered, close it normally first.
        if (open.isPresent() && !contenders(after).isEmpty() && open.get().answeredBy(contenders(after)))
            return closeRound(after, all, open.get(), RocketRound.CloseReason.ALL_ANSWERED, at);
        if (end.isPresent()) {
            List<Player> finished = players.stream()
                    .map(p -> p.playing() ? p.with("FINISHED", p.progress(), at) : p).toList();
            open.ifPresent(round -> {
                round.close(at, RocketRound.CloseReason.MATCH_ENDED, Set.of());
                rounds.save(round);
            });
            matches.savePlayers(finished);
            matches.finish(state.matchId(), state.roomId(), end.get(), at);
            if (open.isPresent()) announceClose(after, open.get(), Map.of(), finished, null, end.get());
            else events.publishEvent(new RocketRoundEvents.Finished(state.roomId(), state.matchId(), end.get()));
            closeRoomIfHostLeft(state, at);
            return after.with(finished, end.get(), at);
        }
        events.publishEvent(new RocketRoundEvents.RankingChanged(state.roomId(), state.matchId()));
        return after;
    }

    /**
     * A room whose host has gone, by leaving the match or after being eliminated, closes when its match ends, as in
     * Wordly, instead of going back to a lobby nobody can start.
     */
    private void closeRoomIfHostLeft(State state, Instant at) {
        matches.departedHostName(state.roomId()).ifPresent(host -> {
            matches.closeRoom(state.roomId(), at);
            events.publishEvent(new RoomClosedEvent(state.roomId(), "Chủ phòng " + host + " đã đóng phòng"));
        });
    }

    private void settle(State state, List<RocketRound> all, Instant now) {
        if (state.finished()) return;
        if (!state.settings().solo()) {
            for (UUID userId : matches.expiredDisconnects(state.matchId(), now)) {
                Optional<Player> player = state.player(userId).filter(Player::playing);
                if (player.isPresent()) state = depart(state, all, player.get(), now);
                if (state.finished()) return;
            }
        }
        var limit = state.settings().timeLimit();
        Optional<RocketRound> open = all.stream().filter(round -> round.status() == RocketRound.Status.OPEN).findFirst();
        if (open.isPresent() && open.get().expired(now)) {
            state = closeRound(state, all, open.get(), RocketRound.CloseReason.DEADLINE, open.get().deadlineAt());
            if (state.finished()) return;
        } else if (open.isPresent()) {
            return;
        }
        State current = state;
        all.stream().filter(round -> round.dueToOpen(now)).findFirst().ifPresent(next -> {
            next.open(now, limit);
            rounds.save(next);
            events.publishEvent(RocketRoundEvents.Started.of(current.roomId(), current.matchId(),
                    current.settings().questionCount(), next, now));
        });
    }

    /** Players still in the round: playing and with lives left. */
    private static Set<UUID> contenders(State state) {
        return state.players().stream().filter(player -> player.playing() && player.progress().alive())
                .map(Player::id).collect(Collectors.toSet());
    }

    /** Closes the round, applies every result, then ends the match or schedules the next round. */
    private State closeRound(State state, List<RocketRound> all, RocketRound round, RocketRound.CloseReason reason, Instant at) {
        Set<UUID> expected = contenders(state);
        Map<UUID, RocketRound.Answer> results = round.close(at, reason, expected);
        List<Player> players = new ArrayList<>();
        for (Player player : state.players()) {
            if (!expected.contains(player.id())) {
                players.add(player);
                continue;
            }
            RocketRound.Answer answer = results.get(player.id());
            RocketPlayerProgress progress = RocketRules.apply(player.progress(), answer.answered(), answer.correct(),
                    answer.points(), answer.responseMs());
            players.add(player.with(progress.alive() ? player.status() : "ELIMINATED", progress, at));
        }
        int closed = (int) all.stream().filter(candidate -> candidate.status() == RocketRound.Status.CLOSED).count();
        List<RocketPlayerProgress> standing = players.stream().filter(player -> !player.forfeited())
                .map(Player::progress).toList();
        Optional<RocketEndReason> end = RocketRules.endReason(state.settings().solo(), state.players().size(), standing,
                closed, state.settings().questionCount());
        rounds.save(round);
        if (end.isPresent()) {
            players = players.stream().map(player -> player.playing() ? player.with("FINISHED", player.progress(), at) : player)
                    .toList();
            matches.savePlayers(players);
            matches.finish(state.matchId(), state.roomId(), end.get(), at);
            announceClose(state, round, results, players, null, end.get());
            if (!state.settings().solo()) closeRoomIfHostLeft(state, at);
            return state.with(players, end.get(), at);
        }
        Instant next = all.stream().filter(candidate -> candidate.sequence() == round.sequence() + 1).findFirst().map(candidate -> {
            candidate.schedule(at.plus(state.settings().revealPause()));
            rounds.save(candidate);
            return candidate.opensAt();
        }).orElse(null);
        matches.savePlayers(players);
        announceClose(state, round, results, players, next, null);
        return state.with(players, null, null);
    }

    /** Tells everyone the answer and each expected player's outcome, with their lives and score after the round. */
    private void announceClose(State state, RocketRound round, Map<UUID, RocketRound.Answer> results, List<Player> players,
            Instant nextRoundAt, RocketEndReason end) {
        List<RocketRoundEvents.PlayerOutcome> outcomes = players.stream().filter(player -> results.containsKey(player.id()))
                .map(player -> {
                    RocketRound.Answer answer = results.get(player.id());
                    return new RocketRoundEvents.PlayerOutcome(player.userId(), answer.optionId(), answer.correct(),
                            answer.points(), player.progress().livesRemaining(), player.progress().totalScore(),
                            player.status());
                }).toList();
        events.publishEvent(new RocketRoundEvents.Closed(state.roomId(), state.matchId(), round.id(),
                round.revealedCorrectOptionId().orElseThrow(), outcomes, nextRoundAt, end != null, end));
    }

    /**
     * The next round is due a reveal pause after this one closed. That schedule is derived from the close rather than
     * read from the next round, whose opensAt moves to the actual opening time, so a replayed answer gets the same value.
     */
    private RoundResult roundResult(State state, List<RocketRound> all, RocketRound round, Player player) {
        Optional<RocketRound.Answer> answer = round.answerOf(player.id());
        boolean hasNext = all.stream().anyMatch(candidate -> candidate.sequence() == round.sequence() + 1);
        Instant next = state.finished() || !hasNext ? null : round.closedAt().plus(state.settings().revealPause());
        return new RoundResult(round.id(), round.revealedCorrectOptionId().orElseThrow(),
                answer.map(RocketRound.Answer::optionId).orElse(null), answer.map(RocketRound.Answer::correct).orElse(false),
                answer.map(RocketRound.Answer::points).orElse(0), player.progress().livesRemaining(),
                player.progress().totalScore(), player.status(), next, state.finished(), state.endReason());
    }

    private static Round view(State state, RocketRound round, Player player, Instant now) {
        Optional<RocketRound.Answer> mine = round.answerOf(player.id()).filter(RocketRound.Answer::answered);
        return new Round(round.id(), round.sequence(), state.settings().questionCount(), round.meaning(),
                round.options().stream().map(option -> new Option(option.optionId(), option.word())).toList(),
                round.opensAt(), round.deadlineAt(), now,
                new Me(mine.isPresent(), mine.map(RocketRound.Answer::optionId).orElse(null)), summary(player));
    }

    private static PlayerSummary summary(Player player) {
        return new PlayerSummary(player.progress().livesRemaining(), player.progress().totalScore(), player.status());
    }

    private static Settings settings(State state) {
        var settings = state.settings();
        return new Settings(settings.questionCount(), settings.initialLives(), settings.secondsPerQuestion());
    }
}
