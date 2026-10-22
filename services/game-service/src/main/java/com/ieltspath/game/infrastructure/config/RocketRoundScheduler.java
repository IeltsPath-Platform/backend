package com.ieltspath.game.infrastructure.config;

import com.ieltspath.game.application.port.RocketMatchStore;
import com.ieltspath.game.application.usecase.RocketGameplayUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Closes Rocket rounds at their deadline and opens the next one after the reveal pause, even if nobody is polling. */
@Component
@EnableScheduling
public class RocketRoundScheduler {
    private static final Logger log = LoggerFactory.getLogger(RocketRoundScheduler.class);

    private final RocketMatchStore matches;
    private final RocketGameplayUseCase gameplay;

    public RocketRoundScheduler(RocketMatchStore matches, RocketGameplayUseCase gameplay) {
        this.matches = matches;
        this.gameplay = gameplay;
    }

    @Scheduled(fixedDelayString = "${game.rocket.round-scan-ms:500}")
    public void scan() {
        for (var matchId : matches.dueMatches(matches.now(), 50)) {
            try {
                gameplay.advance(matchId);
            } catch (RuntimeException e) {
                log.warn("Rocket round advance failed for match {}", matchId, e);
            }
        }
    }
}
