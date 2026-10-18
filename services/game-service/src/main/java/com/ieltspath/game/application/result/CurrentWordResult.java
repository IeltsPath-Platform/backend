package com.ieltspath.game.application.result;

import java.time.Instant;
import java.util.UUID;

public record CurrentWordResult(
        UUID playerWordId,
        int wordNumber,
        Integer wordLimit,
        String meaning,
        int columns,
        int maxAttempts,
        int attemptsUsed,
        boolean timeEnabled,
        Instant deadlineAt,
        String wordStatus,
        String playerStatus,
        int livesRemaining,
        long totalScore,
        int currentStreak,
        int bestStreak,
        java.util.List<Guess> guesses,
        WordlyHintResult hint) {
    public java.util.Map<String, Object> toPayload() {
        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("playerWordId", playerWordId);
        result.put("wordNumber", wordNumber);
        result.put("wordLimit", wordLimit);
        result.put("meaning", meaning);
        result.put("columns", columns);
        result.put("maxAttempts", maxAttempts);
        result.put("attemptsUsed", attemptsUsed);
        result.put("timeEnabled", timeEnabled);
        result.put("deadlineAt", deadlineAt);
        result.put("wordStatus", wordStatus);
        result.put(
                "player",
                java.util.Map.of(
                        "status",
                        playerStatus,
                        "livesRemaining",
                        livesRemaining,
                        "totalScore",
                        totalScore,
                        "currentStreak",
                        currentStreak,
                        "bestStreak",
                        bestStreak));
        result.put("guesses", guesses);
        result.put("keyboard", getKeyboard());
        result.put("hint", hint);
        return result;
    }

    public java.util.Map<String, String> getKeyboard() {
        return com.ieltspath.game.domain.vo.WordlyKeyboard.build(
                guesses.stream().map(Guess::guess).toList(),
                guesses.stream().map(Guess::letters).toList(),
                hint == null ? null : hint.revealedCell().letter());
    }

    public record Guess(int attemptNumber, String guess, java.util.List<String> letters) {}
}
