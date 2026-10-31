package com.ieltspath.game.infrastructure.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Starts the room event subscription and restarts it after Redis was unavailable. */
@Component
class RoomEventChannelSupervisor {
    private static final Logger log = LoggerFactory.getLogger(RoomEventChannelSupervisor.class);

    private final RedisMessageListenerContainer container;

    RoomEventChannelSupervisor(RedisMessageListenerContainer container) {
        this.container = container;
    }

    @Scheduled(fixedDelayString = "${game.room-events.resubscribe-ms:5000}", initialDelayString = "${game.room-events.resubscribe-ms:5000}")
    void ensureSubscribed() {
        if (container.isRunning()) return;
        try {
            container.start();
        } catch (RuntimeException unavailable) {
            log.warn("Room event channel unavailable, retrying: errorType={}", unavailable.getClass().getSimpleName());
        }
    }
}
