package com.group01.game.domain.aggregate;

import com.group01.game.domain.exception.InvalidGameSessionStateException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameSessionTest {
    @Test
    void recordsScoreAndCompletesAfterEverySnapshotItemIsAnswered() {
        GameSession session = new GameSession(UUID.randomUUID(), UUID.randomUUID(), null, null,
                "WORD_MEANING_MATCH", "VOCABULARY", "PRACTICE", Instant.parse("2026-09-23T00:00:00Z"),
                Map.of("items", List.of(Map.of("id", "one"), Map.of("id", "two"))), "PENDING");

        session.recordAnswer(true, Instant.parse("2026-09-23T00:00:01Z"));
        assertEquals(GameSessionStatus.IN_PROGRESS, session.status());
        session.recordAnswer(false, Instant.parse("2026-09-23T00:00:02Z"));

        assertEquals(GameSessionStatus.COMPLETED, session.status());
        assertEquals(1, session.score());
        assertEquals(Instant.parse("2026-09-23T00:00:02Z"), session.endedAt());
        assertThrows(InvalidGameSessionStateException.class,
                () -> session.recordAnswer(true, Instant.parse("2026-09-23T00:00:03Z")));
    }
}
