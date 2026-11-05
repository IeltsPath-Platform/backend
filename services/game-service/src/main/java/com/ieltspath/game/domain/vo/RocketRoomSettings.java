package com.ieltspath.game.domain.vo;

import java.time.Duration;
import java.util.Set;

/**
 * Rocket room settings. Solo players only pick the number of questions; lives and time use the defaults.
 * Multiplayer hosts choose all three within the limits below. There is no endless mode.
 */
public record RocketRoomSettings(boolean solo, int questionCount, int initialLives, int secondsPerQuestion,
        WordFilter wordFilter) implements RoomSettings {
    public static final int DEFAULT_LIVES = 4;
    public static final int DEFAULT_SECONDS = 15;
    public static final int DEFAULT_MULTIPLAYER_QUESTIONS = 10;
    public static final Set<Integer> SECOND_CHOICES = Set.of(10, 15, 20);
    private static final int MAX_QUESTIONS = 20;

    public RocketRoomSettings(boolean solo, int questionCount, int initialLives, int secondsPerQuestion) {
        this(solo, questionCount, initialLives, secondsPerQuestion, WordFilter.ANY);
    }

    public RocketRoomSettings {
        if (wordFilter == null) wordFilter = WordFilter.ANY;
        int minQuestions = solo ? 3 : 5;
        if (questionCount < minQuestions || questionCount > MAX_QUESTIONS)
            throw new IllegalArgumentException("Question count must be between " + minQuestions + " and " + MAX_QUESTIONS);
        if (initialLives < 3 || initialLives > 6) throw new IllegalArgumentException("Initial lives must be between 3 and 6");
        if (!SECOND_CHOICES.contains(secondsPerQuestion))
            throw new IllegalArgumentException("Seconds per question must be 10, 15 or 20");
    }

    public static RocketRoomSettings solo(int questionCount) {
        return new RocketRoomSettings(true, questionCount, DEFAULT_LIVES, DEFAULT_SECONDS);
    }

    public static RocketRoomSettings multiplayer(int questionCount, int initialLives, int secondsPerQuestion) {
        return new RocketRoomSettings(false, questionCount, initialLives, secondsPerQuestion);
    }

    @Override public RoomGame game() { return RoomGame.ROCKET; }
    @Override public int maxPlayers() { return solo ? 1 : 4; }

    public RocketRoomSettings withWordFilter(WordFilter filter) {
        return new RocketRoomSettings(solo, questionCount, initialLives, secondsPerQuestion, filter);
    }

    public Duration timeLimit() { return Duration.ofSeconds(secondsPerQuestion); }

    /** Pause between a round closing and the next opening, long enough for the reveal animation. */
    public Duration revealPause() { return solo ? Duration.ofMillis(2500) : Duration.ofSeconds(3); }
}
