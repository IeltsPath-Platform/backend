package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.RewardGrantRepository;
import com.ieltspath.commonobservability.Correlation;
import com.ieltspath.commonobservability.OutboxTraceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcRewardGrantRepository implements RewardGrantRepository {
    private static final int MAX_ERROR_LENGTH = 200;

    private final JdbcTemplate jdbc;
    private final OutboxTraceContext traceContext;

    @Autowired
    public JdbcRewardGrantRepository(JdbcTemplate jdbc, OutboxTraceContext traceContext) {
        this.jdbc = jdbc;
        this.traceContext = traceContext;
    }

    public JdbcRewardGrantRepository(JdbcTemplate jdbc) {
        this(jdbc, null);
    }

    @Override
    public void enqueue(UUID userId, String reason, String referenceId, int amount) {
        jdbc.update("""
                INSERT INTO reward_grants (id, user_id, amount, reason, reference_id, correlation_id, trace_context)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, reason, reference_id) DO NOTHING
                """, UUID.randomUUID(), userId, amount, reason, referenceId, Correlation.current(),
                traceContext == null ? null : traceContext.capture());
    }

    @Override
    public List<PendingGrant> claimDue(int limit, Instant resendBefore) {
        return jdbc.query("""
                SELECT id, user_id, amount, reason, reference_id, attempts, correlation_id, trace_context FROM reward_grants
                WHERE status = 'PENDING' AND (attempts = 0 OR updated_at < ?)
                ORDER BY created_at LIMIT ? FOR UPDATE SKIP LOCKED
                """, (rows, index) -> new PendingGrant(rows.getObject("id", UUID.class),
                rows.getObject("user_id", UUID.class), rows.getInt("amount"), rows.getString("reason"),
                rows.getString("reference_id"), rows.getInt("attempts"), rows.getString("correlation_id"),
                rows.getString("trace_context")),
                Timestamp.from(resendBefore), limit);
    }

    @Override
    public void markSent(UUID id, Instant now) {
        jdbc.update("UPDATE reward_grants SET attempts = attempts + 1, updated_at = ? WHERE id = ?",
                Timestamp.from(now), id);
    }

    @Override
    public void markFailed(UUID id, String error, Instant now) {
        settle(id, "FAILED", error, now);
    }

    @Override
    public boolean settle(UUID id, String status, String error, Instant now) {
        String kept = error == null ? null : error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
        return jdbc.update("UPDATE reward_grants SET status = ?, last_error = ?, updated_at = ? "
                + "WHERE id = ? AND status = 'PENDING'", status, kept, Timestamp.from(now), id) > 0;
    }
}
