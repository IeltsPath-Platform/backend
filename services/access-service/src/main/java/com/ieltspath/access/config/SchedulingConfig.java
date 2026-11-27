package com.ieltspath.access.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Runs the {@code @Scheduled} jobs of the service: outbox relay and purge, subscription lapse and expiry notices, user
 * deletion and wallet reconciliation. Without this none of them runs.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
