package com.ieltspath.game.application.port;

import com.ieltspath.game.application.result.SoloReply;

import java.time.Instant;
import java.util.*;

public interface SoloGameplayRepository {
    record Word(
            UUID id,
            int sequence,
            String answer,
            String status,
            int attempts,
            Instant started,
            Instant deadline) {}

    record State(
            UUID matchId,
            UUID roomId,
            String matchStatus,
            String playerMode,
            String mode,
            Integer limit,
            boolean timed,
            int initialLives,
            Instant started,
            Instant ended,
            String endReason,
            UUID playerId,
            UUID userId,
            String displayName,
            String playerStatus,
            int lives,
            long score,
            long base,
            long bonus,
            int streak,
            int best,
            int processed,
            int solved,
            long solveMs,
            Word word) {}

    record Receipt(
            UUID id,
            UUID matchId,
            UUID wordId,
            UUID userId,
            String guess,
            Instant received,
            SoloReply reply) {}

    State lock(UUID matchId, UUID userId);

    Receipt receive(UUID matchId, UUID wordId, UUID userId, UUID requestId, String guess);

    Receipt receipt(UUID requestId);

    List<Receipt> pending(UUID matchId);

    void respond(UUID requestId, SoloReply reply);

    boolean valid(UUID matchId, String guess);

    void guess(
            UUID wordId,
            UUID requestId,
            int number,
            String guess,
            List<String> letters,
            boolean correct,
            Instant received);

    void closeWord(
            UUID wordId,
            String status,
            int attempts,
            Instant finished,
            Long solveMs,
            long base,
            long bonus);

    void progress(
            State state,
            int lives,
            long score,
            long base,
            long bonus,
            int streak,
            int best,
            int processed,
            int solved,
            long solveMs,
            Instant scoreReceivedAt);

    void advance(State state, Instant now);

    void finish(State state, String playerStatus, String reason, Instant now, boolean closeRoom);

    Map<String, Object> result(UUID matchId, UUID userId);

    List<UUID> due(int limit);

    UUID owner(UUID matchId);

    List<UUID> participants(UUID matchId);

    void disconnected(UUID matchId, UUID userId, Instant at);

    void reconnect(UUID matchId, UUID userId, Instant at);

    boolean reconnectExpired(UUID matchId, UUID userId, Instant at);

    String closureMessage(UUID roomId);
}
