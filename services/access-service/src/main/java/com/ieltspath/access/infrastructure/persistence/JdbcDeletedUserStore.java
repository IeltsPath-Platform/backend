package com.ieltspath.access.infrastructure.persistence;

import com.ieltspath.access.application.port.DeletedUserStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcDeletedUserStore implements DeletedUserStore {
    private final JdbcTemplate jdbc;

    public JdbcDeletedUserStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean isDeleted(UUID userId) {
        if (userId == null) return false;
        Integer found = jdbc.query(
                "SELECT 1 FROM deleted_users WHERE user_id = ?",
                rs -> rs.next() ? 1 : null,
                userId);
        return found != null;
    }

    @Override
    public void recordDeletion(UUID eventId, UUID userId, Instant occurredAt) {
        Timestamp ts = Timestamp.from(occurredAt);
        jdbc.update("""
                INSERT INTO deleted_users (user_id, deleted_at)
                VALUES (?, ?)
                ON CONFLICT (user_id) DO NOTHING
                """, userId, ts);
        jdbc.update("""
                INSERT INTO user_deletion_jobs (event_id, user_id, status, created_at)
                VALUES (?, ?, 'PENDING', ?)
                ON CONFLICT (event_id) DO NOTHING
                """, eventId, userId, ts);
    }

    public record Job(UUID eventId, UUID userId) { }

    public List<Job> claimPendingJobs(int limit) {
        return jdbc.query("""
                SELECT event_id, user_id
                FROM user_deletion_jobs
                WHERE status = 'PENDING'
                ORDER BY created_at ASC
                FOR UPDATE SKIP LOCKED
                LIMIT ?
                """, (rs, rowNum) -> new Job(
                UUID.fromString(rs.getString("event_id")),
                UUID.fromString(rs.getString("user_id"))), limit);
    }

    /**
     * Keeps financial ledger, key activations, and credit history per retention policy; closes active subscriptions,
     * zeroes wallet balance, and deletes email verification state.
     */
    public void applyAccountDeletion(UUID userId) {
        jdbc.update("""
                UPDATE subscriptions
                SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND status = 'ACTIVE'
                """, userId);
        jdbc.update("""
                UPDATE point_wallets
                SET balance = 0, updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                """, userId);
        jdbc.update("DELETE FROM user_email_verification_state WHERE user_id = ?", userId);
    }

    public void markJobDone(UUID eventId) {
        jdbc.update("""
                UPDATE user_deletion_jobs
                SET status = 'DONE', completed_at = CURRENT_TIMESTAMP
                WHERE event_id = ?
                """, eventId);
    }
}
