package com.ieltspath.assessment.config;

import com.ieltspath.commonobservability.OutboxMeterBinder;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/** Publishes how many outbox events are still waiting for the broker, and for how long. */
@Configuration
class OutboxMetricsConfig {
    @Bean
    MeterBinder outboxMetrics(JdbcTemplate jdbc) {
        return new OutboxMeterBinder(jdbc, "assessment", "outbox_events", "published_at IS NULL");
    }
}
