package com.ieltspath.game.domain.exception;

public class GameRoomNotFoundException extends RuntimeException {
    private final Object roomRef;

    public GameRoomNotFoundException(Object roomRef) {
        super("Không tìm thấy phòng chơi");
        this.roomRef = roomRef;
    }

    public Object roomRef() {
        return roomRef;
    }
}
