package com.ieltspath.game.domain.aggregate;

import com.ieltspath.game.domain.exception.InvalidGameSessionStateException;
import com.ieltspath.game.domain.vo.WordlyKeyboard;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameTypeAnswerAndSessionTest {

    private static GameSession session() {
        return new GameSession(UUID.randomUUID(), UUID.randomUUID(), null, null, "WORD_MEANING_MATCH", "VOCABULARY",
                "PRACTICE", Instant.parse("2026-09-23T00:00:00Z"), Map.of("items", List.of(Map.of("id", "one"))), "PENDING");
    }

    private static GameAnswer answer(int sequence, long duration, Map<String, Object> snapshot) {
        return new GameAnswer(UUID.randomUUID(), UUID.randomUUID(), sequence, null, null, snapshot, Map.of(), true, duration);
    }

    @Test
    void aSupportedTypeAndDomainAreAccepted() {
        assertEquals(GameType.WORD_MEANING_MATCH, GameType.require("WORD_MEANING_MATCH", "VOCABULARY"));
        assertEquals(GameType.SENTENCE_COMPLETION, GameType.require("SENTENCE_COMPLETION", "GRAMMAR"));
    }

    @Test
    void anUnknownOrMissingTypeIsRejected() {
        for (String type : new String[]{"CHESS", null}) {
            var error = assertThrows(IllegalArgumentException.class, () -> GameType.require(type, "VOCABULARY"));
            assertEquals("Unsupported gameType", error.getMessage());
        }
    }

    @Test
    void aTypeUsedWithAnotherDomainIsRejected() {
        var error = assertThrows(IllegalArgumentException.class, () -> GameType.require("SENTENCE_COMPLETION", "VOCABULARY"));
        assertEquals("gameType is unsupported for learningDomain", error.getMessage());
    }

    @Test
    void anAnswerNeedsASequenceFromOneAndANonNegativeDuration() {
        assertEquals(1, answer(1, 0, Map.of()).itemSequence());
        var zeroSequence = assertThrows(IllegalArgumentException.class, () -> answer(0, 0, Map.of()));
        assertEquals("itemSequence must be positive", zeroSequence.getMessage());
        var negative = assertThrows(IllegalArgumentException.class, () -> answer(1, -1, Map.of()));
        assertEquals("durationMilliseconds must not be negative", negative.getMessage());
    }

    @Test
    void anAnswerKeepsACopyOfItsSnapshot() {
        Map<String, Object> snapshot = new HashMap<>(Map.of("prompt", "word"));
        var saved = answer(1, 10, snapshot);
        snapshot.put("prompt", "changed");
        assertEquals("word", saved.itemSnapshot().get("prompt"));
        assertThrows(UnsupportedOperationException.class, () -> saved.itemSnapshot().put("x", "y"));
    }

    @Test
    void aSessionInProgressCanBeAbandonedOnce() {
        var session = session();
        Instant at = Instant.parse("2026-09-23T00:00:05Z");
        session.abandon(at);
        assertEquals(GameSessionStatus.ABANDONED, session.status());
        assertEquals(at, session.endedAt());
        assertThrows(InvalidGameSessionStateException.class, () -> session.abandon(at.plusSeconds(1)));
    }

    @Test
    void theKeyboardKeepsTheBestColourOfEachLetterAndMarksTheHintCorrect() {
        var keys = WordlyKeyboard.build(List.of("EAR", "AIR"),
                List.of(List.of("ABSENT", "PRESENT", "CORRECT"), List.of("PRESENT", "ABSENT", "CORRECT")), null);
        assertEquals(Map.of("A", "PRESENT", "E", "ABSENT", "I", "ABSENT", "R", "CORRECT"), keys);
        var withHint = WordlyKeyboard.build(List.of("EAR"), List.of(List.of("ABSENT", "PRESENT", "CORRECT")), "A");
        assertEquals("CORRECT", withHint.get("A"));
    }
}
