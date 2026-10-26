package com.ieltspath.library.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class LibraryTimeConfig {
    @Bean
    Clock libraryClock() { return Clock.systemUTC(); }
}
