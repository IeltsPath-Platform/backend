package com.group01.game.domain.exception;

public class InvalidGameSessionStateException extends RuntimeException {
    public InvalidGameSessionStateException(String message) {
        super(message);
    }
}
