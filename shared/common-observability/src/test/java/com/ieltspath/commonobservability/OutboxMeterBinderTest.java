package com.ieltspath.commonobservability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class OutboxMeterBinderTest {
    @Test
    void pendingAndOldestAgeGaugesReadTheTable() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), org.mockito.ArgumentMatchers.<Class<Number>>any()))
                .thenAnswer(call -> call.<String>getArgument(0).contains("count(") ? 3L : 42.5d);
        var registry = new SimpleMeterRegistry();

        new OutboxMeterBinder(jdbc, "assessment", "outbox_events", "published_at IS NULL").bindTo(registry);

        Gauge pending = registry.get("outbox_pending").tag("service", "assessment").gauge();
        Gauge oldest = registry.get("outbox_oldest_age_seconds").tag("service", "assessment").gauge();
        assertEquals(3.0, pending.value());
        assertEquals(42.5, oldest.value());
    }

    @Test
    void aFailingQueryReportsNaNInsteadOfBreakingTheScrape() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), org.mockito.ArgumentMatchers.<Class<Number>>any()))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("down"));
        var registry = new SimpleMeterRegistry();

        new OutboxMeterBinder(jdbc, "community", "outbox_events", "published_at IS NULL AND failed_at IS NULL")
                .bindTo(registry);

        assertTrue(Double.isNaN(registry.get("outbox_pending").tag("service", "community").gauge().value()));
    }
}
