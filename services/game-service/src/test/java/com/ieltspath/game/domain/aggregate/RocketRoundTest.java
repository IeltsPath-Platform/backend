package com.ieltspath.game.domain.aggregate;

import static org.junit.jupiter.api.Assertions.*;

import com.ieltspath.game.domain.exception.SoloGameplayException;
import com.ieltspath.game.domain.vo.RocketOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class RocketRoundTest {
    private final Instant opens = Instant.parse("2026-10-22T00:00:00Z");
    private final RocketOption driver = new RocketOption(UUID.randomUUID(), UUID.randomUUID(), "driver");
    private final RocketOption actor = new RocketOption(UUID.randomUUID(), UUID.randomUUID(), "actor");
    private final RocketOption career = new RocketOption(UUID.randomUUID(), UUID.randomUUID(), "career");
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    private RocketRound openRound() {
        var round = new RocketRound(UUID.randomUUID(), UUID.randomUUID(), 1, actor.sourceWordId(), "Diễn viên (nam)",
                List.of(driver, actor, career), actor.optionId());
        round.open(opens, Duration.ofSeconds(15));
        return round;
    }

    private static void assertCode(String code, Executable action) {
        assertEquals(code, assertThrows(SoloGameplayException.class, action).code());
    }

    @Test
    void scoresOnArrivalButRevealsOnlyAfterClosing() {
        var round = openRound();
        assertEquals(opens.plusSeconds(15), round.deadlineAt());
        var answer = round.accept(alice, actor.optionId(), "r1", opens.plusSeconds(3));
        assertTrue(answer.correct());
        assertEquals(180, answer.points());
        assertEquals(3000, answer.responseMs());
        assertTrue(round.revealedCorrectOptionId().isEmpty());
        assertFalse(round.answeredBy(Set.of(alice, bob)));
        var wrong = round.accept(bob, career.optionId(), "r2", opens.plusSeconds(5));
        assertFalse(wrong.correct());
        assertEquals(0, wrong.points());
        assertTrue(round.answeredBy(Set.of(alice, bob)));
        var results = round.close(opens.plusSeconds(5), RocketRound.CloseReason.ALL_ANSWERED, Set.of(alice, bob));
        assertEquals(2, results.size());
        assertEquals(actor.optionId(), round.revealedCorrectOptionId().orElseThrow());
    }

    @Test
    void retriesReplayButSecondAnswersAndForeignRequestsAreRejected() {
        var round = openRound();
        var first = round.accept(alice, driver.optionId(), "r1", opens.plusSeconds(2));
        assertSame(first, round.accept(alice, driver.optionId(), "r1", opens.plusSeconds(9)));
        assertCode("REQUEST_ID_CONFLICT", () -> round.accept(alice, actor.optionId(), "r1", opens.plusSeconds(9)));
        assertCode("ALREADY_ANSWERED", () -> round.accept(alice, actor.optionId(), "r2", opens.plusSeconds(9)));
        assertCode("INVALID_OPTION", () -> round.accept(bob, UUID.randomUUID(), "r3", opens.plusSeconds(9)));
    }

    @Test
    void answersAtOrAfterTheDeadlineAreRejected() {
        var round = openRound();
        assertCode("ROUND_CLOSED", () -> round.accept(alice, actor.optionId(), "late", opens.plusSeconds(15)));
        assertTrue(round.expired(opens.plusSeconds(15)));
        assertFalse(round.expired(opens.plusMillis(14_999)));
        assertEquals(100, round.accept(bob, actor.optionId(), "edge", opens.plusMillis(14_999)).points());
    }

    @Test
    void closingFillsMissesAndHappensOnlyOnce() {
        var round = openRound();
        round.accept(alice, actor.optionId(), "r1", opens.plusSeconds(1));
        var results = round.close(opens.plusSeconds(15), RocketRound.CloseReason.DEADLINE, Set.of(alice, bob));
        assertTrue(results.get(alice).answered());
        var miss = results.get(bob);
        assertFalse(miss.answered());
        assertFalse(miss.correct());
        assertEquals(0, miss.points());
        assertThrows(IllegalStateException.class,
                () -> round.close(opens.plusSeconds(16), RocketRound.CloseReason.DEADLINE, Set.of(alice, bob)));
        assertCode("ROUND_CLOSED", () -> round.accept(bob, actor.optionId(), "r9", opens.plusSeconds(16)));
        assertSame(results.get(alice), round.accept(alice, actor.optionId(), "r1", opens.plusSeconds(30)));
    }

    @Test
    void lifecycleAndShapeAreGuarded() {
        var round = new RocketRound(UUID.randomUUID(), UUID.randomUUID(), 2, actor.sourceWordId(), "Diễn viên (nam)",
                List.of(driver, actor, career), actor.optionId());
        assertCode("ROUND_CLOSED", () -> round.accept(alice, actor.optionId(), "early", opens));
        assertThrows(IllegalStateException.class, () -> round.close(opens, RocketRound.CloseReason.DEADLINE, Set.of(alice)));
        round.open(opens, Duration.ofSeconds(10));
        assertThrows(IllegalStateException.class, () -> round.open(opens, Duration.ofSeconds(10)));
        assertThrows(IllegalArgumentException.class, () -> new RocketRound(UUID.randomUUID(), UUID.randomUUID(), 1,
                actor.sourceWordId(), "x", List.of(driver, actor), actor.optionId()));
        assertThrows(IllegalArgumentException.class, () -> new RocketRound(UUID.randomUUID(), UUID.randomUUID(), 1,
                actor.sourceWordId(), "x", List.of(driver, actor, career), UUID.randomUUID()));
    }
}
