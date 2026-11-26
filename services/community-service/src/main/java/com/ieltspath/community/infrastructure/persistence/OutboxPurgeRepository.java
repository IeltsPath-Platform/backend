package com.ieltspath.community.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class OutboxPurgeRepository {
    private final JdbcTemplate jdbc;

    public OutboxPurgeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public int deletePublishedBefore(Instant cutoff, int batchSize) {
        return jdbc.update("""
                DELETE FROM outbox_events WHERE id IN (
                    SELECT id FROM outbox_events
                    WHERE published_at IS NOT NULL AND failed_at IS NULL AND published_at < ?
                    ORDER BY published_at
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED)
                """, Timestamp.from(cutoff), batchSize);
    }
}
