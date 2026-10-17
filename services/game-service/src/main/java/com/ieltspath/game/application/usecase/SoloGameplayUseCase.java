package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.port.*;
import com.ieltspath.game.application.port.SoloGameplayRepository.*;
import com.ieltspath.game.application.result.*;
import com.ieltspath.game.domain.vo.WordlyRoundRules;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.*;
import java.util.*;

@Service
public class SoloGameplayUseCase {
 private final java.time.Clock clock;
    private final SoloGameplayRepository store;
    private final WordlyMatchStore words;
    private final TransactionTemplate tx;
    private final ApplicationEventPublisher events;

    public SoloGameplayUseCase(
            SoloGameplayRepository store,
            WordlyMatchStore words,
            PlatformTransactionManager manager,
            ApplicationEventPublisher events, Clock clock) {
        this.clock=clock;
        this.store = store;
        this.words = words;
        this.tx = new TransactionTemplate(manager);
        this.events = events;
    }

    public SoloReply submit(UUID matchId, UUID wordId, UUID userId, UUID requestId, String input) {
        String guess = input.trim().toUpperCase(Locale.ROOT);
        if (!guess.matches("[A-Z]{1,100}"))
            return error(400, "INVALID_CHARACTERS", "Use letters A–Z only", matchId);
        var receipt = store.receive(matchId, wordId, userId, requestId, guess);
        if (receipt.reply() != null) return receipt.reply();
        return tx.execute(
                t -> {
                    process(matchId, userId);
                    var reply = store.receipt(requestId).reply();
                    return reply == null
                            ? error(
                                    202,
                                    "REQUEST_PENDING",
                                    "Request is queued for processing",
                                    matchId)
                            : reply;
                });
    }

    private void process(UUID matchId, UUID userId) {
        store.lock(matchId, userId);
        for (var receipt : store.pending(matchId)) {
            State state = store.lock(matchId, receipt.userId());
            SoloReply reply = apply(state, receipt);
            store.respond(receipt.id(), reply);
        }
    }

    private SoloReply apply(State s, Receipt r) {
        if (!"IN_PROGRESS".equals(s.matchStatus()))
            return error(409, "MATCH_FINISHED", "Match has finished", s.matchId());
        if (!playing(s)) return error(409,"PLAYER_NOT_ACTIVE","Player cannot submit guesses",s.matchId());
        if (store.reconnectExpired(s.matchId(), s.userId(), r.received())) {
            forfeit(s, r.received());
            publish(store.lock(s.matchId(), s.userId()));
            return error(409, "PLAYER_FORFEITED", "Reconnect deadline has passed", s.matchId());
        }
        Word w = s.word();
        if (w == null || !w.id().equals(r.wordId()) || !"ACTIVE".equals(w.status()))
            return error(
                    409, "WORD_NOT_CURRENT", "Guess does not target the active word", s.matchId());
        if (w.deadline() != null && !r.received().isBefore(w.deadline())) {
            settle(s, "TIMED_OUT", w.attempts(), r.received());
            var body =
                    new LinkedHashMap<>(
                            error(409, "WORD_EXPIRED", "Word deadline has passed", s.matchId())
                                    .body());
            State updated = store.lock(s.matchId(), s.userId());
            body.put("nextWord", current(updated));
            body.put("matchFinished", !"IN_PROGRESS".equals(updated.matchStatus()));
            publish(updated);
            return new SoloReply(409, body);
        }
        if (r.guess().length() != w.answer().length())
            return error(
                    400,
                    "INVALID_GUESS_LENGTH",
                    "Guess length does not match this word",
                    s.matchId());
        if (!r.guess().equals(w.answer()) && !store.valid(s.matchId(), r.guess()))
            return error(
                    400,
                    "WORD_NOT_FOUND",
                    "Word was not found in this match's dictionary",
                    s.matchId());
        var letters = WordlyRoundRules.evaluate(w.answer(), r.guess());
        boolean solved = w.answer().equals(r.guess());
        int attempt = w.attempts() + 1;
        store.guess(w.id(), r.id(), attempt, r.guess(), letters, solved, r.received());
        var board = words.current(s.matchId(), s.userId()).orElseThrow();
        String status =
                solved ? "SOLVED" : attempt >= w.answer().length() + 1 ? "FAILED" : "ACTIVE";
        var reward =
                WordlyRoundRules.reward(
                        solved, s.streak(), w.started(), w.deadline(), r.received());
        if ("ACTIVE".equals(status)) store.closeWord(w.id(), status, attempt, null, null, 0, 0);
        else settle(s, status, attempt, r.received());
        State updated = store.lock(s.matchId(), s.userId());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requestId", r.id());
        body.put("playerWordId", w.id());
        body.put("guess", r.guess());
        body.put("letters", letters);
        body.put("keyboard", board.getKeyboard());
        body.put("attemptsUsed", attempt);
        body.put("maxAttempts", w.answer().length() + 1);
        body.put("wordStatus", status);
        body.put(
                "points",
                Map.of(
                        "base",
                        reward.base(),
                        "bonus",
                        reward.bonus(),
                        "total",
                        reward.base() + reward.bonus()));
        body.put("player", player(updated));
        body.put("nextWord", "ACTIVE".equals(status) ? null : current(updated));
        body.put("matchFinished", !"IN_PROGRESS".equals(updated.matchStatus()));
        body.put("endReason", updated.endReason());
        publish(updated);
        return new SoloReply(200, body);
    }

    private void settle(State s, String status, int attempts, Instant received) {
        settle(s, status, attempts, received, true);
    }

    private void settle(State s, String status, int attempts, Instant received, boolean advance) {
        Word w = s.word();
        boolean solved = "SOLVED".equals(status);
        var reward =
                WordlyRoundRules.reward(solved, s.streak(), w.started(), w.deadline(), received);
        store.closeWord(
                w.id(),
                status,
                attempts,
                received,
                solved ? reward.solveMs() : null,
                reward.base(),
                reward.bonus());
        int lives = s.lives() - (solved ? 0 : 1);
        int processed = s.processed() + 1;
        store.progress(
                s,
                lives,
                Math.addExact(s.score(), reward.base() + reward.bonus()),
                s.base() + reward.base(),
                s.bonus() + reward.bonus(),
                reward.streak(),
                Math.max(s.best(), reward.streak()),
                processed,
                s.solved() + (solved ? 1 : 0),
                s.solveMs() + reward.solveMs(),
                received);
        if (lives <= 0) store.finish(s, "ELIMINATED", "NO_LIVES_LEFT", received, false);
        else if (s.limit() != null && processed >= s.limit())
            store.finish(s, "FINISHED", "WORD_LIMIT_REACHED", received, false);
        else if (advance) store.advance(s, clock.instant());
    }

    private Map<String, Object> player(State s) {
        return Map.of(
                "status",
                s.playerStatus(),
                "livesRemaining",
                s.lives(),
                "totalScore",
                s.score(),
                "currentStreak",
                s.streak(),
                "bestStreak",
                s.best());
    }

    private boolean playing(State s) {
        return "ACTIVE".equals(s.playerStatus()) || "DISCONNECTED".equals(s.playerStatus());
    }

    private void forfeit(State s, Instant now) {
        if (s.word() != null && "ACTIVE".equals(s.word().status()))
            store.closeWord(s.word().id(), "CANCELLED", s.word().attempts(), now, null, 0, 0);
        store.finish(s, "FORFEITED", "PLAYER_LEFT", now, true);
        if("MULTIPLAYER".equals(s.playerMode()))
            events.publishEvent(new WordlyPlayerLeftEvent(s.roomId(),s.matchId(),s.userId(),
                s.displayName()==null?s.userId().toString():s.displayName()));
    }

    private Map<String, Object> current(State s) {
        return "IN_PROGRESS".equals(s.matchStatus())
                ? words.current(s.matchId(), s.userId())
                        .map(CurrentWordResult::toPayload)
                        .orElse(null)
                : null;
    }

    private void publish(State s) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
                "type", "MULTIPLAYER".equals(s.playerMode()) || "IN_PROGRESS".equals(s.matchStatus())
                        ? "WORD_STATE" : "MATCH_FINISHED");
        payload.put("matchId", s.matchId());
        payload.put("player", player(s));
        payload.put("currentWord", current(s));
        payload.put("endReason", s.endReason());
        events.publishEvent(new SoloGameplayEvent(s.roomId(), s.userId(), payload));
        if ("MULTIPLAYER".equals(s.playerMode()))
            events.publishEvent(new WordlyMatchUpdatedEvent(s.roomId(), s.matchId(), s.userId(),
                    !"IN_PROGRESS".equals(s.matchStatus()), store.closureMessage(s.roomId())));
    }

    private SoloReply error(int status, String code, String message, UUID matchId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", clock.instant());
        body.put("status", status);
        body.put("error", status == 400 ? "Bad Request" : status == 409 ? "Conflict" : "Accepted");
        body.put("code", code);
        body.put("message", message);
        body.put("path", "/api/games/matches/" + matchId + "/guesses");
        body.put("details", null);
        return new SoloReply(status, body);
    }

    public SoloReply leave(UUID matchId, UUID userId) {
        return tx.execute(
                t -> {
                    process(matchId, userId);
                    while (!store.pending(matchId).isEmpty()) process(matchId, userId);
                    State s = store.lock(matchId, userId);
                    if ("IN_PROGRESS".equals(s.matchStatus())) {
                        Instant now = clock.instant();
                        if (playing(s) && s.word() != null
                                && "ACTIVE".equals(s.word().status())
                                && s.word().deadline() != null
                                && !now.isBefore(s.word().deadline())) {
                            settle(s, "TIMED_OUT", s.word().attempts(), now, false);
                            s = store.lock(matchId, userId);
                        }
                        if ("IN_PROGRESS".equals(s.matchStatus())) {
                            if (!"FORFEITED".equals(s.playerStatus())) forfeit(s, now);
                        }
                    }
                    s = store.lock(matchId, userId);
                    publish(s);
                    Map<String,Object> body=new LinkedHashMap<>();
                    body.put("matchId",matchId);
                    body.put("status",s.matchStatus());
                    body.put("endReason",s.endReason());
                    body.put("playerStatus",s.playerStatus());
                    body.put("totalScore",s.score());
                    body.put("resultAvailable",!"IN_PROGRESS".equals(s.matchStatus()));
                    return new SoloReply(200,body);
                });
    }

    public Map<String, Object> result(UUID matchId, UUID userId) {
        return store.result(matchId, userId);
    }

    public void timeout(UUID matchId, UUID userId) {
        tx.executeWithoutResult(
                t -> {
                    process(matchId, userId);
                    State s = store.lock(matchId, userId);
                    if (!store.pending(matchId).isEmpty()) return;
                    if ("IN_PROGRESS".equals(s.matchStatus()) && playing(s)
                            && store.reconnectExpired(matchId, userId, clock.instant())) {
                        forfeit(s, clock.instant());
                        publish(store.lock(matchId, userId));
                        return;
                    }
                    if ("IN_PROGRESS".equals(s.matchStatus())
                            && playing(s)
                            && s.word() != null
                            && "ACTIVE".equals(s.word().status())
                            && s.word().deadline() != null
                            && !clock.instant().isBefore(s.word().deadline())) {
                        settle(s, "TIMED_OUT", s.word().attempts(), clock.instant());
                        publish(store.lock(matchId, userId));
                    }
                });
    }

    public void disconnected(UUID matchId, UUID userId) {
        tx.executeWithoutResult(t -> {
            State s = store.lock(matchId, userId);
            if ("MULTIPLAYER".equals(s.playerMode()) && "IN_PROGRESS".equals(s.matchStatus()) && playing(s)) {
                store.disconnected(matchId, userId, clock.instant());
                publish(store.lock(matchId, userId));
            }
        });
    }

    public void reconnect(UUID matchId, UUID userId) {
        timeout(matchId, userId);
        tx.executeWithoutResult(t -> {
            State s = store.lock(matchId, userId);
            if ("MULTIPLAYER".equals(s.playerMode()) && "IN_PROGRESS".equals(s.matchStatus()) && playing(s)) {
                Instant now = clock.instant();
                if (store.reconnectExpired(matchId, userId, now)) forfeit(s, now);
                else store.reconnect(matchId, userId, now);
                publish(store.lock(matchId, userId));
            }
        });
    }
}
