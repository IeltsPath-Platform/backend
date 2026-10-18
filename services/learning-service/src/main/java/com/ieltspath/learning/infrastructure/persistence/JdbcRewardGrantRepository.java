package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.RewardGrantRepository;
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

    public JdbcRewardGrantRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void enqueue(UUID userId, String reason, String referenceId, int amount) {
        jdbc.update("""
                INSERT INTO reward_grants (id, user_id, amount, reason, reference_id)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (user_id, reason, reference_id) DO NOTHING
                """, UUID.randomUUID(), userId, amount, reason, referenceId);
    }

    @Override
    public List<PendingGrant> claimDue(int limit, Instant resendBefore) {
        return jdbc.query("""
                SELECT id, user_id, amount, reason, reference_id, attempts FROM reward_grants
                WHERE status = 'PENDING' AND (attempts = 0 OR updated_at < ?)
                ORDER BY created_at LIMIT ? FOR UPDATE SKIP LOCKED
                """, (rows, index) -> new PendingGrant(rows.getObject("id", UUID.class),
                rows.getObject("user_id", UUID.class), rows.getInt("amount"), rows.getString("reason"),
                rows.getString("reference_id"), rows.getInt("attempts")), Timestamp.from(resendBefore), limit);
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
