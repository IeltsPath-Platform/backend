package com.ieltspath.game.domain.vo;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Games played through rooms (lobby, ready, start). Legacy session games such as WORD_MEANING_MATCH are not room
 * games. A user belongs to at most one room across all of these games.
 */
public enum RoomGame {
    WORDLY,
    ROCKET;

    public static Optional<RoomGame> of(String gameType) {
        return Arrays.stream(values()).filter(game -> game.name().equals(gameType)).findFirst();
    }

    public static boolean isRoomGame(String gameType) {
        return of(gameType).isPresent();
    }

    /** Stored `game_type` values, for queries that span every room game. */
    public static List<String> storedTypes() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }
}
