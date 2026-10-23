package com.ieltspath.content.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableConfigurationProperties(ContentAiProperties.class)
public class AiDraftConfiguration {

    /**
     * The clock decides which day an allowance belongs to. Using the configured zone keeps the day boundary the same
     * on every host, whatever the server's own timezone is.
     */
    @Bean
    Clock aiClock(ContentAiProperties properties) {
        return Clock.system(ZoneId.of(properties.getAi().getQuotaZone()));
    }
}
