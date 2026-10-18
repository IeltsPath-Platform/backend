package com.ieltspath.user.config;

import java.time.Clock;
import org.springframework.context.annotation.*;

@Configuration
public class StudyTrackingConfig {
    @Bean
    public Clock studyTrackingClock() { return Clock.systemUTC(); }
}
