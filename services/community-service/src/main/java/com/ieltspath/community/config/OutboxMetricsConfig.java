package com.ieltspath.community.config;

import com.ieltspath.commonobservability.OutboxMeterBinder;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/** Publishes how many outbox events are still waiting for the broker; events that failed for good are not counted. */
@Configuration
class OutboxMetricsConfig {
    @Bean
    MeterBinder outboxMetrics(JdbcTemplate jdbc) {
        return new OutboxMeterBinder(jdbc, "community", "outbox_events", "published_at IS NULL AND failed_at IS NULL");
    }
}
