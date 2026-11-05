package com.ieltspath.commonobservability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Exposes how much of an outbox table is waiting to be relayed and how old the oldest waiting row is. The SQL counts rows
 * matching {@code pendingPredicate}, so a service states what "waiting" means for its own table. A failed read reports
 * NaN so that one unavailable database never breaks the metrics scrape.
 */
public class OutboxMeterBinder implements MeterBinder {
    private static final Logger log = LoggerFactory.getLogger(OutboxMeterBinder.class);

    private final JdbcTemplate jdbc;
    private final String service;
    private final String table;
    private final String pendingPredicate;

    public OutboxMeterBinder(JdbcTemplate jdbc, String service, String table, String pendingPredicate) {
        this.jdbc = jdbc;
        this.service = service;
        this.table = table;
        this.pendingPredicate = pendingPredicate;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("outbox_pending", this, binder -> binder.read(
                        "select count(*) from " + table + " where " + pendingPredicate))
                .tag("service", service)
                .description("Outbox rows waiting to be relayed")
                .register(registry);
        Gauge.builder("outbox_oldest_age_seconds", this, binder -> binder.read(
                        "select coalesce(extract(epoch from now() - min(created_at)), 0) from " + table
                                + " where " + pendingPredicate))
                .tag("service", service)
                .description("Age of the oldest outbox row waiting to be relayed")
                .register(registry);
    }

    private double read(String sql) {
        try {
            Number value = jdbc.queryForObject(sql, Number.class);
            return value == null ? 0d : value.doubleValue();
        } catch (RuntimeException unavailable) {
            log.warn("Outbox metric not read: service={}, errorType={}", service, unavailable.getClass().getSimpleName());
            return Double.NaN;
        }
    }
}
