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

    public record Job(UUID eventId, UUID userId, int attempts) { }

    public List<Job> claimPendingJobs(int limit, long leaseSeconds, int maxAttempts) {
        // A job that crashed on its last attempt would stay PROCESSING for ever; once its lease is over it is FAILED.
        jdbc.update("""
                UPDATE user_deletion_jobs
                SET status = 'FAILED', completed_at = CURRENT_TIMESTAMP, last_error = COALESCE(last_error, 'LEASE_EXPIRED')
                WHERE status = 'PROCESSING' AND attempts >= ?
                  AND claimed_at < CURRENT_TIMESTAMP - make_interval(secs => ?)
                """, maxAttempts, leaseSeconds);
        return jdbc.query("""
                UPDATE user_deletion_jobs
                SET status = 'PROCESSING',
                    claimed_at = CURRENT_TIMESTAMP,
                    attempts = attempts + 1
                WHERE event_id IN (
                    SELECT event_id
                    FROM user_deletion_jobs
                    WHERE (status = 'PENDING'
                           OR (status = 'PROCESSING' AND claimed_at < CURRENT_TIMESTAMP - make_interval(secs => ?)))
                      AND attempts < ?
                    ORDER BY created_at ASC
                    FOR UPDATE SKIP LOCKED
                    LIMIT ?
                )
                RETURNING event_id, user_id, attempts
                """, (rs, rowNum) -> new Job(
                UUID.fromString(rs.getString("event_id")),
                UUID.fromString(rs.getString("user_id")),
                rs.getInt("attempts")), leaseSeconds, maxAttempts, limit);
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
        // The remaining balance is written off through the ledger, so the wallet and the ledger still add up.
        // The key is fixed per user, so a retried job writes the entry once.
        jdbc.update("""
                WITH closed AS (
                    SELECT user_id, balance FROM point_wallets WHERE user_id = ? AND balance > 0 FOR UPDATE
                ), entry AS (
                    INSERT INTO point_ledger_entries (user_id, delta, balance_after, transaction_type, reference_type,
                                                      reference_id, idempotency_key, description)
                    SELECT user_id, -balance, 0, 'ADMIN_ADJUSTMENT', 'ACCOUNT_DELETION', user_id,
                           'account-deletion:' || user_id, 'Xóa số dư khi người dùng xóa tài khoản'
                    FROM closed
                    ON CONFLICT (idempotency_key) DO NOTHING
                    RETURNING user_id
                )
                UPDATE point_wallets w
                SET balance = 0, total_debited = w.total_debited + c.balance, updated_at = CURRENT_TIMESTAMP
                FROM closed c
                WHERE w.user_id = c.user_id
                """, userId);
        jdbc.update("DELETE FROM user_email_verification_state WHERE user_id = ?", userId);
    }

    public void markJobDone(UUID eventId) {
        jdbc.update("""
                UPDATE user_deletion_jobs
                SET status = 'DONE', completed_at = CURRENT_TIMESTAMP, last_error = NULL
                WHERE event_id = ?
                """, eventId);
    }

    public void markJobFailed(UUID eventId, String errorCode) {
        jdbc.update("""
                UPDATE user_deletion_jobs
                SET status = 'FAILED', last_error = ?, completed_at = CURRENT_TIMESTAMP
                WHERE event_id = ?
                """, errorCode, eventId);
    }

    public void recordJobError(UUID eventId, String errorCode) {
        jdbc.update("""
                UPDATE user_deletion_jobs
                SET last_error = ?
                WHERE event_id = ?
                """, errorCode, eventId);
    }
}
