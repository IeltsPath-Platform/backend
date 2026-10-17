package com.ieltspath.game.application.result;
import java.util.UUID;
public record RoomClosedEvent(UUID roomId, String message) {}
