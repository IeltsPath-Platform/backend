package com.ieltspath.learning.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class LearningClockConfig {
    @Bean
    public Clock learningClock() {
        return Clock.systemUTC();
    }
}
