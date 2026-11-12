package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.port.GameAnswerEvaluator;
import com.ieltspath.game.application.port.GameMatchEventWriter;
import com.ieltspath.game.domain.aggregate.GameAnswer;
import com.ieltspath.game.domain.aggregate.GameMatch;
import com.ieltspath.game.domain.aggregate.GameMatchPlayer;
import com.ieltspath.game.domain.aggregate.GameSession;
import com.ieltspath.game.domain.repository.GameMatchPlayerRepository;
import com.ieltspath.game.domain.repository.GameMatchRepository;
import com.ieltspath.game.domain.repository.GameSessionRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SubmitGameAnswerUseCaseTest {
    private final GameSessionRepository sessions = mock(GameSessionRepository.class);
    private final GameAnswerEvaluator evaluator = mock(GameAnswerEvaluator.class);
    private final GameMatchEventWriter events = mock(GameMatchEventWriter.class);
    private final GameMatchRepository matches = mock(GameMatchRepository.class);
    private final GameMatchPlayerRepository players = mock(GameMatchPlayerRepository.class);
    private final SubmitGameAnswerUseCase submit = new SubmitGameAnswerUseCase(sessions, evaluator, events, matches, players);
    private final UUID user = UUID.randomUUID();
    private final Map<String, Object> response = Map.of("answer", "a");

    private GameSession session(UUID playerId) {
        var session = new GameSession(UUID.randomUUID(), user, null, playerId,
                "WORD_MEANING_MATCH", "VOCABULARY", "PRACTICE", Instant.now().minusSeconds(30),
                Map.of("items", List.of(Map.of("answerSpecJson", "answer-spec"))), "PENDING");
        when(sessions.findOwned(session.id(), user)).thenReturn(Optional.of(session));
        when(sessions.save(session)).thenReturn(session);
        when(evaluator.isCorrect("answer-spec", "a")).thenReturn(true);
        return session;
    }

    @Test
    void theLastAnswerUpdatesPlayerProgressRecordsTheLiveEventAndCompletesTheMatch() {
        UUID matchId = UUID.randomUUID(), playerId = UUID.randomUUID();
        GameSession session = session(playerId);
        Instant joined = session.startedAt();
        var match = new GameMatch(matchId, UUID.randomUUID(), "WORD_MEANING_MATCH", "VOCABULARY", Map.of(), Map.of(), joined);
        var player = new GameMatchPlayer(playerId, matchId, UUID.randomUUID(), user,
                0, null, GameMatchPlayer.Status.ACTIVE, joined, null);
        var opponent = new GameMatchPlayer(UUID.randomUUID(), matchId, UUID.randomUUID(), UUID.randomUUID(),
                2, null, GameMatchPlayer.Status.FINISHED, joined, joined.plusSeconds(20));
        Map<UUID, GameMatchPlayer> saved = new LinkedHashMap<>();
        saved.put(player.id(), player);
        saved.put(opponent.id(), opponent);
        when(players.findById(playerId)).thenAnswer(call -> Optional.of(saved.get(playerId)));
        when(players.findByMatchId(matchId)).thenAnswer(call -> new ArrayList<>(saved.values()));
        when(players.save(any())).thenAnswer(call -> {
            GameMatchPlayer updated = call.getArgument(0);
            saved.put(updated.id(), updated);
            return updated;
        });
        when(matches.findForUpdate(matchId)).thenReturn(Optional.of(match));

        var result = submit.execute(session.id(), user, 1, response, 750);

        assertTrue(result.correct());
        assertEquals(1, result.score());
        assertEquals("COMPLETED", result.sessionStatus());
        assertFalse(result.duplicate());
        assertEquals(GameMatchPlayer.Status.FINISHED, saved.get(playerId).status());
        assertEquals(1, saved.get(playerId).score());
        verify(sessions).saveAnswer(any(GameAnswer.class));
        verify(events).recordAnswer(eq(matchId), eq(playerId), any(GameAnswer.class), eq(session), any(Instant.class));
        verify(players).saveAll(argThat(ranked -> ranked.size() == 2
                && ranked.get(0).id().equals(opponent.id()) && ranked.get(0).rank() == 1
                && ranked.get(1).id().equals(playerId) && ranked.get(1).rank() == 2));
        assertEquals(GameMatch.Status.COMPLETED, match.status());
        verify(matches).save(match);
    }

    @Test
    void practiceAnswersSaveTheirScoreWithoutAccessingMatchState() {
        GameSession session = session(null);

        var result = submit.execute(session.id(), user, 1, response, 400);

        assertEquals(1, result.score());
        assertEquals("COMPLETED", result.sessionStatus());
        verify(sessions).saveAnswer(any(GameAnswer.class));
        verify(sessions).save(session);
        verifyNoInteractions(players, matches, events);
    }

    @Test
    void retryingTheSameAnswerDoesNotRecordProgressOrEventsAgain() {
        GameSession session = session(UUID.randomUUID());
        session.recordAnswer(true, Instant.now());
        var existing = new GameAnswer(UUID.randomUUID(), session.id(), 1, null, null,
                Map.of("answerSpecJson", "answer-spec"), response, true, 400);
        when(sessions.findAnswer(session.id(), 1)).thenReturn(Optional.of(existing));

        var result = submit.execute(session.id(), user, 1, response, 400);

        assertTrue(result.duplicate());
        assertEquals(existing.id(), result.answerId());
        assertEquals(1, result.score());
        verify(sessions, never()).saveAnswer(any());
        verify(sessions, never()).save(any());
        verifyNoInteractions(players, matches, events);
    }
}
