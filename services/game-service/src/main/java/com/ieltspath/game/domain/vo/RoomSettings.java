package com.ieltspath.game.domain.vo;

/** Settings a room keeps for its game; each room game validates its own values. */
public sealed interface RoomSettings permits WordlyRoomSettings {
    RoomGame game();

    boolean solo();

    int maxPlayers();
}
