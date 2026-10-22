package com.ieltspath.game.domain.vo;

/** A Rocket player's running totals; {@code correctTimeMs} breaks score ties in the ranking. */
public record RocketPlayerProgress(int livesRemaining, long totalScore, int roundsPlayed, int correctCount,
        long correctTimeMs) {
    public RocketPlayerProgress {
        if (livesRemaining < 0 || totalScore < 0 || roundsPlayed < 0 || correctCount < 0 || correctTimeMs < 0)
            throw new IllegalArgumentException("Progress values cannot be negative");
        if (correctCount > roundsPlayed) throw new IllegalArgumentException("Correct answers exceed rounds played");
    }

    public static RocketPlayerProgress start(int initialLives) {
        return new RocketPlayerProgress(initialLives, 0, 0, 0, 0);
    }

    public boolean alive() { return livesRemaining > 0; }
}
