package com.ieltspath.game.domain.exception;

public class InvalidGameRoomStateException extends RuntimeException {
    public InvalidGameRoomStateException(String message) {
        super(message);
    }
}
