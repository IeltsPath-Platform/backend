package com.ieltspath.game.domain.vo;

public record WordlyRoomSettings(PlayerMode playerMode, int initialLives, PlayMode playMode, Integer wordLimit, boolean timeEnabled,
        WordFilter wordFilter) implements RoomSettings {
    public static final int MIN_WORDS = 5;

    public WordlyRoomSettings(PlayerMode playerMode, int initialLives, PlayMode playMode, Integer wordLimit, boolean timeEnabled) {
        this(playerMode, initialLives, playMode, wordLimit, timeEnabled, WordFilter.ANY);
    }
    public WordlyRoomSettings(PlayerMode playerMode, int initialLives, PlayMode playMode, Integer wordLimit) {
        this(playerMode, initialLives, playMode, wordLimit, true);
    }
    public enum PlayerMode { SOLO, MULTIPLAYER }
    public enum PlayMode { FINITE, ENDLESS }

    public WordlyRoomSettings {
        if (wordFilter == null) wordFilter = WordFilter.ANY;
        if (playerMode == null || playMode == null) throw new IllegalArgumentException("Player mode and play mode are required");
        if (playerMode == PlayerMode.MULTIPLAYER && !timeEnabled) throw new IllegalArgumentException("Multiplayer requires a timer");
        if (initialLives < 3 || initialLives > 6) throw new IllegalArgumentException("Initial lives must be between 3 and 6");
        if (playMode == PlayMode.FINITE && (wordLimit == null || wordLimit < 5)) {
            throw new IllegalArgumentException("Finite mode requires at least 5 words");
        }
        if (playMode == PlayMode.ENDLESS && wordLimit != null) {
            throw new IllegalArgumentException("Endless mode must not have a word limit");
        }
    }

    @Override public RoomGame game() { return RoomGame.WORDLY; }
    @Override public boolean solo() { return playerMode == PlayerMode.SOLO; }
    @Override public int maxPlayers() { return playerMode == PlayerMode.SOLO ? 1 : 4; }
    /** Words the catalogue must hold for a match: the word limit, or a minimum for endless mode. */
    public int requiredWords() { return wordLimit == null ? MIN_WORDS : wordLimit; }
    public WordlyRoomSettings withWordFilter(WordFilter filter) {
        return new WordlyRoomSettings(playerMode, initialLives, playMode, wordLimit, timeEnabled, filter);
    }
    public Integer wordTimeLimitSeconds() { return timeEnabled ? 180 : null; }
}
