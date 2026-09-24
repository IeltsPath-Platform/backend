package com.group01.game.application.usecase;

import com.group01.game.application.port.GameAnswerEvaluator;
import com.group01.game.application.port.GameMatchEventWriter;
import com.group01.game.application.port.OutboxWriter;
import com.group01.game.application.result.GameAnswerResult;
import com.group01.game.domain.aggregate.GameAnswer;
import com.group01.game.domain.aggregate.GameMatchPlayer;
import com.group01.game.domain.aggregate.GameSession;
import com.group01.game.domain.aggregate.GameSessionStatus;
import com.group01.game.domain.exception.GameMatchNotFoundException;
import com.group01.game.domain.exception.GameSessionNotFoundException;
import com.group01.game.domain.repository.GameMatchPlayerRepository;
import com.group01.game.domain.repository.GameMatchRepository;
import com.group01.game.domain.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class SubmitGameAnswerUseCase {
    private final GameSessionRepository sessionRepository;
    private final GameAnswerEvaluator answerEvaluator;
    private final OutboxWriter outboxWriter;
    private final GameMatchEventWriter matchEventWriter;
    private final GameMatchRepository matchRepository;
    private final GameMatchPlayerRepository matchPlayerRepository;

    public SubmitGameAnswerUseCase(GameSessionRepository sessionRepository, GameAnswerEvaluator answerEvaluator,
                                   OutboxWriter outboxWriter, GameMatchEventWriter matchEventWriter,
                                   GameMatchRepository matchRepository,
                                   GameMatchPlayerRepository matchPlayerRepository) {
        this.sessionRepository = sessionRepository;
        this.answerEvaluator = answerEvaluator;
        this.outboxWriter = outboxWriter;
        this.matchEventWriter = matchEventWriter;
        this.matchRepository = matchRepository;
        this.matchPlayerRepository = matchPlayerRepository;
    }

    @Transactional
    public GameAnswerResult execute(UUID sessionId, UUID userId, int itemSequence,
                                    Map<String, Object> responsePayload, long durationMilliseconds) {
        GameSession session = sessionRepository.findOwned(sessionId, userId)
                .orElseThrow(() -> new GameSessionNotFoundException(sessionId));
        var existing = sessionRepository.findAnswer(sessionId, itemSequence);
        if (existing.isPresent()) {
            GameAnswer answer = existing.get();
            if (Objects.equals(answer.responsePayload(), responsePayload)) {
                return new GameAnswerResult(answer.id(), itemSequence, answer.correct(), session.score(),
                        session.status().name(), true);
            }
            throw new IllegalStateException("This item has already been answered with a different response");
        }
        if (itemSequence < 1 || itemSequence > session.itemCount()) {
            throw new IllegalArgumentException("itemSequence is outside this session");
        }
        if (durationMilliseconds < 0) throw new IllegalArgumentException("durationMilliseconds must not be negative");
        Object rawItems = session.sourceSnapshot().get("items");
        Map<String, Object> item = (Map<String, Object>) ((List<?>) rawItems).get(itemSequence - 1);
        boolean correct = answerEvaluator.isCorrect(item.get("answerSpecJson"), responsePayload.get("answer"));
        session.recordAnswer(correct, Instant.now());
        GameAnswer answer = new GameAnswer(UUID.randomUUID(), sessionId, itemSequence,
                asUuid(item.get("vocabularySenseId")), asUuid(item.get("questionVersionId")),
                item, responsePayload, correct, durationMilliseconds);
        sessionRepository.saveAnswer(answer);
        GameSession saved = sessionRepository.save(session);
        boolean matchCompleted = session.matchPlayerId() != null && updateMatchAfterAnswer(answer, saved);
        outboxWriter.append("GameSession", sessionId.toString(), "GameAnswerSubmitted",
                Map.of("sessionId", sessionId.toString(), "itemSequence", itemSequence,
                        "isCorrect", correct, "score", saved.score()));
        if (matchCompleted) {
            outboxWriter.append("GameMatch", session.matchPlayerId().toString(), "GameMatchCompleted",
                    Map.of("matchPlayerId", session.matchPlayerId().toString()));
        }
        return new GameAnswerResult(answer.id(), itemSequence, correct, saved.score(), saved.status().name(), false);
    }

    private boolean updateMatchAfterAnswer(GameAnswer answer, GameSession session) {
        UUID matchPlayerId = session.matchPlayerId();
        GameMatchPlayer initialPlayer = matchPlayerRepository.findById(matchPlayerId)
                .orElseThrow(() -> new GameMatchNotFoundException(matchPlayerId));
        var match = matchRepository.findForUpdate(initialPlayer.matchId())
                .orElseThrow(() -> new GameMatchNotFoundException(initialPlayer.matchId()));
        if (match.status() != com.group01.game.domain.aggregate.GameMatch.Status.IN_PROGRESS) {
            throw new IllegalStateException("Match is no longer in progress");
        }

        Instant now = Instant.now();
        GameMatchPlayer player = matchPlayerRepository.findById(matchPlayerId)
                .orElseThrow(() -> new GameMatchNotFoundException(matchPlayerId));
        matchPlayerRepository.save(player.withProgress(
                session.score(), session.status() == GameSessionStatus.COMPLETED, now));
        matchEventWriter.recordAnswer(match.id(), player.id(), answer, session, now);

        List<GameMatchPlayer> players = matchPlayerRepository.findByMatchId(match.id());
        boolean completed = !players.isEmpty()
                && players.stream().allMatch(p -> p.status() == GameMatchPlayer.Status.FINISHED);
        if (!completed) return false;

        List<GameMatchPlayer> sortedPlayers = players.stream()
                .sorted(Comparator.comparingInt(GameMatchPlayer::score).reversed()
                        .thenComparingLong(GameMatchPlayer::durationMilliseconds)
                        .thenComparing(p -> p.userId().toString()))
                .toList();
        List<GameMatchPlayer> rankedPlayers = java.util.stream.IntStream.range(0, sortedPlayers.size())
                .mapToObj(index -> sortedPlayers.get(index).withRank(index + 1))
                .toList();
        matchPlayerRepository.saveAll(rankedPlayers);
        match.complete(now);
        matchRepository.save(match);
        return true;
    }

    private UUID asUuid(Object value) { return value == null ? null : UUID.fromString(value.toString()); }
}
