package com.ieltspath.game.application.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WordlyRankingResult(UUID matchId, String status, String playerMode,
        boolean isFinal, Instant serverTime, List<Player> players) {
    public WordlyRankingResult {
        players = List.copyOf(players);
    }

    public record Player(UUID userId, String displayName, Integer slotNumber, Integer rank,
            String status, int livesRemaining, long totalScore, long totalSolveTimeMs,
            int wordsProcessed, int wordsSolved, int currentStreak, Instant reconnectDeadlineAt) {
        /** reconnectDeadlineAt is set only while a DISCONNECTED player is inside the 30 s grace. */
        public Player(UUID userId, String displayName, Integer slotNumber, Integer rank, String status,
                int livesRemaining, long totalScore, long totalSolveTimeMs, int wordsProcessed, int wordsSolved,
                int currentStreak) {
            this(userId, displayName, slotNumber, rank, status, livesRemaining, totalScore, totalSolveTimeMs,
                    wordsProcessed, wordsSolved, currentStreak, null);
        }
    }
}
