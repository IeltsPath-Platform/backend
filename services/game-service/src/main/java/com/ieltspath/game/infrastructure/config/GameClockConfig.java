package com.ieltspath.game.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GameClockConfig {
    @Bean
    public Clock gameClock() { return Clock.systemUTC(); }
}
