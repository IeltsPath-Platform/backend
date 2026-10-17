package com.ieltspath.game.infrastructure.config;

import com.ieltspath.game.application.port.SoloGameplayRepository;
import com.ieltspath.game.application.usecase.SoloGameplayUseCase;

import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class SoloTimeoutScheduler {
    private final SoloGameplayRepository store;
    private final SoloGameplayUseCase gameplay;

    public SoloTimeoutScheduler(SoloGameplayRepository store, SoloGameplayUseCase gameplay) {
        this.store = store;
        this.gameplay = gameplay;
    }

    @Scheduled(fixedDelayString = "${game.wordly.timeout-scan-ms:1000}")
    public void scan() {
        for (var id : store.due(50)) {
            try {
                for (var userId : store.participants(id)) gameplay.timeout(id, userId);
            } catch (RuntimeException e) {
                org.slf4j.LoggerFactory.getLogger(getClass())
                        .warn("Wordly timeout failed for match {}", id, e);
            }
        }
    }
}
