package com.group01.game.domain.exception;

public class InvalidGameRoomStateException extends RuntimeException {
    public InvalidGameRoomStateException(String message) {
        super(message);
    }
}
