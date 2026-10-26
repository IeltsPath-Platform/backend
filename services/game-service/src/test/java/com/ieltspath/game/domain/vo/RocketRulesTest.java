package com.ieltspath.game.domain.vo;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RocketRulesTest {
    private final Instant deadline = Instant.parse("2026-10-22T00:00:15Z");
    private final Duration limit = Duration.ofSeconds(15);

    @Test
    void correctAnswersEarnOneHundredPlusSpeedBonus() {
        assertEquals(200, RocketRules.points(true, deadline.minus(limit), deadline, limit));
        assertEquals(180, RocketRules.points(true, deadline.minusSeconds(12), deadline, limit));
        assertEquals(106, RocketRules.points(true, deadline.minusMillis(1000), deadline, limit));
        assertEquals(100, RocketRules.points(true, deadline, deadline, limit));
        assertEquals(100, RocketRules.points(true, deadline.plusSeconds(3), deadline, limit));
        assertEquals(200, RocketRules.points(true, deadline.minusSeconds(40), deadline, limit));
        assertEquals(0, RocketRules.points(false, deadline.minusSeconds(14), deadline, limit));
    }

    @Test
    void aMissCostsOneLifeAndACorrectAnswerKeepsLives() {
        var start = RocketPlayerProgress.start(4);
        var hit = RocketRules.apply(start, true, true, 150, 5000);
        assertEquals(new RocketPlayerProgress(4, 150, 1, 1, 5000), hit);
        var wrong = RocketRules.apply(hit, true, false, 0, 2000);
        assertEquals(new RocketPlayerProgress(3, 150, 2, 1, 5000), wrong);
        var timeout = RocketRules.apply(wrong, false, false, 0, 0);
        assertEquals(2, timeout.livesRemaining());
        var out = RocketRules.apply(RocketRules.apply(timeout, false, false, 0, 0), false, false, 0, 0);
        assertFalse(out.alive());
        assertThrows(IllegalStateException.class, () -> RocketRules.apply(out, true, true, 100, 0));
    }

    @Test
    void soloEndsOnNoLivesBeforeTheLastRoundCounts() {
        var alive = new RocketPlayerProgress(2, 300, 3, 2, 0);
        var dead = new RocketPlayerProgress(0, 300, 6, 2, 0);
        assertEquals(Optional.empty(), RocketRules.endReason(true, 1, List.of(alive), 3, 6));
        assertEquals(Optional.of(RocketEndReason.ALL_ROUNDS_COMPLETED), RocketRules.endReason(true, 1, List.of(alive), 6, 6));
        assertEquals(Optional.of(RocketEndReason.NO_LIVES_LEFT), RocketRules.endReason(true, 1, List.of(dead), 6, 6));
        assertEquals(Optional.of(RocketEndReason.PLAYER_LEFT), RocketRules.endReason(true, 1, List.of(), 2, 6));
    }

    @Test
    void multiplayerEndsWhenOneOrNoPlayerSurvives() {
        var alive = new RocketPlayerProgress(1, 100, 4, 1, 0);
        var dead = new RocketPlayerProgress(0, 50, 4, 1, 0);
        assertEquals(Optional.empty(), RocketRules.endReason(false, 3, List.of(alive, alive, dead), 4, 10));
        assertEquals(Optional.of(RocketEndReason.LAST_PLAYER_STANDING),
                RocketRules.endReason(false, 3, List.of(alive, dead, dead), 4, 10));
        assertEquals(Optional.of(RocketEndReason.ALL_ELIMINATED), RocketRules.endReason(false, 2, List.of(dead, dead), 4, 10));
        assertEquals(Optional.of(RocketEndReason.ALL_PLAYERS_LEFT), RocketRules.endReason(false, 2, List.of(), 4, 10));
        assertEquals(Optional.of(RocketEndReason.ALL_ROUNDS_COMPLETED),
                RocketRules.endReason(false, 2, List.of(alive, alive), 10, 10));
    }

    @Test
    void settingsFollowTheAgreedLimits() {
        var solo = RocketRoomSettings.solo(6);
        assertEquals(RoomGame.ROCKET, solo.game());
        assertTrue(solo.solo());
        assertEquals(1, solo.maxPlayers());
        assertEquals(4, solo.initialLives());
        assertEquals(Duration.ofSeconds(15), solo.timeLimit());
        assertEquals(Duration.ofMillis(2500), solo.revealPause());
        var multi = RocketRoomSettings.multiplayer(10, 3, 20);
        assertEquals(4, multi.maxPlayers());
        assertEquals(Duration.ofSeconds(3), multi.revealPause());
        assertDoesNotThrow(() -> RocketRoomSettings.solo(3));
        assertThrows(IllegalArgumentException.class, () -> RocketRoomSettings.solo(2));
        assertThrows(IllegalArgumentException.class, () -> RocketRoomSettings.multiplayer(4, 4, 15));
        assertThrows(IllegalArgumentException.class, () -> RocketRoomSettings.multiplayer(21, 4, 15));
        assertThrows(IllegalArgumentException.class, () -> RocketRoomSettings.multiplayer(10, 7, 15));
        assertThrows(IllegalArgumentException.class, () -> RocketRoomSettings.multiplayer(10, 4, 12));
    }
}
