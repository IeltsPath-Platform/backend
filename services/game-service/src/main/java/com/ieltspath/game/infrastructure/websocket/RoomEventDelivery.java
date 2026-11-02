package com.ieltspath.game.infrastructure.websocket;

/** Hands a room event to the WebSocket sessions connected to this instance. */
@FunctionalInterface
public interface RoomEventDelivery {
    void deliver(Object event);
}
