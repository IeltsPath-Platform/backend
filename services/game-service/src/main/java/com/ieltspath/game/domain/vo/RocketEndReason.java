package com.ieltspath.game.domain.vo;

/** Why a Rocket match ended. The first three apply to solo play; the others to multiplayer. */
public enum RocketEndReason {
    ALL_ROUNDS_COMPLETED,
    NO_LIVES_LEFT,
    PLAYER_LEFT,
    LAST_PLAYER_STANDING,
    ALL_ELIMINATED,
    ALL_PLAYERS_LEFT
}
