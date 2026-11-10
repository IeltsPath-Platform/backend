package com.ieltspath.user.config;

import com.ieltspath.commonobservability.OutboxMeterBinder;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
class OutboxMetricsConfig {
    @Bean MeterBinder userOutboxMetrics(JdbcTemplate jdbc) {
        return new OutboxMeterBinder(jdbc, "user", "outbox_events", "status = 'PENDING'");
    }
}
