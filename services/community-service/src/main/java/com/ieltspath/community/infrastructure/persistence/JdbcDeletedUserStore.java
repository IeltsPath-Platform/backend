package com.ieltspath.community.infrastructure.persistence;

import com.ieltspath.community.application.port.DeletedUserStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcDeletedUserStore implements DeletedUserStore {
    public static final UUID DELETED_AUTHOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

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
     * Anonymizes posts and comments to the deleted-author sentinel (keeping post content and images intact per policy)
     * anonymises the reporter of their reports, and deletes personal bookmarks, follows, reactions and blocks.
     */
    public void anonymizeAndDeleteCommunityData(UUID userId) {
        jdbc.update("UPDATE posts SET author_id = ? WHERE author_id = ?", DELETED_AUTHOR_ID, userId);
        jdbc.update("UPDATE comments SET author_id = ? WHERE author_id = ?", DELETED_AUTHOR_ID, userId);
        jdbc.update("DELETE FROM post_bookmarks WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM user_follows WHERE follower_id = ? OR followee_id = ?", userId, userId);
        jdbc.update("DELETE FROM post_reactions WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM comment_reactions WHERE user_id = ?", userId);
        // Reports stay as moderation history with the reporter anonymised. A sentinel row already filed for the same
        // content (an earlier deleted reporter) would break the one-report-per-reporter rule, so that duplicate goes.
        jdbc.update("""
                DELETE FROM community_reports r
                WHERE r.reporter_id = ? AND EXISTS (
                    SELECT 1 FROM community_reports s
                    WHERE s.reporter_id = ? AND s.content_type = r.content_type AND s.content_id = r.content_id)
                """, userId, DELETED_AUTHOR_ID);
        jdbc.update("UPDATE community_reports SET reporter_id = ? WHERE reporter_id = ?", DELETED_AUTHOR_ID, userId);
        jdbc.update("DELETE FROM user_blocks WHERE blocker_id = ? OR blocked_id = ?", userId, userId);
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
