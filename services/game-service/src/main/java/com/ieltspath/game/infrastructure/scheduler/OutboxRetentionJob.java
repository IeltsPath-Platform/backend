package com.ieltspath.game.infrastructure.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

/**
 * Game publishes no outbox events, so its outbox works as a log that is kept for a while and then removed. Rows older than
 * the retention window are deleted once a day. Two instances may run the delete at the same time; deleting the same old
 * rows twice does no harm, so no lock is taken.
 */
@Component
public class OutboxRetentionJob {
    private final JdbcTemplate jdbc;
    private final Duration retention;

    public OutboxRetentionJob(JdbcTemplate jdbc, @Value("${game.outbox.retention-days:30}") int retentionDays) {
        this.jdbc = jdbc;
        this.retention = Duration.ofDays(retentionDays);
    }

    @Scheduled(cron = "${game.outbox.retention-cron:0 15 4 * * *}")
    public void deleteExpiredRows() {
        runOnce(Instant.now());
    }

    /** @return the number of rows deleted. */
    public int runOnce(Instant now) {
        return jdbc.update("DELETE FROM outbox_events WHERE created_at < ?", Timestamp.from(now.minus(retention)));
    }
}
