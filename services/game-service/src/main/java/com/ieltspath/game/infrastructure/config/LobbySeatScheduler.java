package com.ieltspath.game.infrastructure.config;

import com.ieltspath.game.application.usecase.LobbySeatUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Removes lobby members whose reconnect grace has passed. Each seat is settled under its room lock. */
@Component
public class LobbySeatScheduler {
    private final LobbySeatUseCase seats;

    public LobbySeatScheduler(LobbySeatUseCase seats) {
        this.seats = seats;
    }

    @Scheduled(fixedDelayString = "${game.lobby.seat-scan-ms:1000}")
    public void scan() {
        for (var seat : seats.due(50)) {
            try {
                seats.expire(seat.roomId(), seat.userId());
            } catch (RuntimeException e) {
                org.slf4j.LoggerFactory.getLogger(getClass())
                        .warn("Lobby seat expiry failed for room {}", seat.roomId(), e);
            }
        }
    }
}
