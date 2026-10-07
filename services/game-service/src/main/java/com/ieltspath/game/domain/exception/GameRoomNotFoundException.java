package com.ieltspath.game.domain.exception;

public class GameRoomNotFoundException extends RuntimeException {
    public GameRoomNotFoundException(Object id) { super("Game room not found: " + id); }
}
