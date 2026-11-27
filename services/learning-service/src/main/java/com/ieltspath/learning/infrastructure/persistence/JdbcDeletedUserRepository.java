package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.DeletedUserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcDeletedUserRepository implements DeletedUserRepository {
    private final JdbcTemplate jdbc;

    public JdbcDeletedUserRepository(JdbcTemplate jdbc) {
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

    public record SpeakingRecordingRef(UUID id, String objectKey) { }

    public List<Job> claimPendingJobs(int limit, long leaseSeconds, int maxAttempts) {
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

    public List<SpeakingRecordingRef> findSpeakingRecordingsBatch(UUID userId, int limit) {
        return jdbc.query("""
                SELECT r.id, r.object_key
                FROM speaking_recordings r
                JOIN speaking_submissions s ON s.id = r.submission_id
                WHERE s.user_id = ? AND r.object_key IS NOT NULL
                ORDER BY r.id ASC
                LIMIT ?
                """, (rs, rowNum) -> new SpeakingRecordingRef(
                UUID.fromString(rs.getString("id")),
                rs.getString("object_key")), userId, limit);
    }

    public void deleteSpeakingRecordingsByIds(List<UUID> ids) {
        if (ids.isEmpty()) return;
        for (UUID id : ids) {
            jdbc.update("DELETE FROM speaking_recordings WHERE id = ?", id);
        }
    }

    public int deleteBatchByUserId(String tableName, String userColumn, UUID userId, int limit) {
        String sql = "DELETE FROM " + tableName + " WHERE ctid IN (SELECT ctid FROM " + tableName
                + " WHERE " + userColumn + " = ? LIMIT ?)";
        return jdbc.update(sql, userId, limit);
    }

    public void deleteSpeakingRecordingsForUser(UUID userId) {
        jdbc.update("""
                DELETE FROM speaking_analyses
                WHERE submission_id IN (SELECT id FROM speaking_submissions WHERE user_id = ?)
                """, userId);
        jdbc.update("""
                DELETE FROM speaking_grading_contexts
                WHERE submission_id IN (SELECT id FROM speaking_submissions WHERE user_id = ?)
                """, userId);
        jdbc.update("""
                DELETE FROM speaking_recordings
                WHERE submission_id IN (SELECT id FROM speaking_submissions WHERE user_id = ?)
                """, userId);
        jdbc.update("DELETE FROM speaking_submissions WHERE user_id = ?", userId);
    }

    public void deleteDiagnosticsForUser(UUID userId) {
        jdbc.update("""
                DELETE FROM diagnostic_probes
                WHERE session_id IN (SELECT id FROM diagnostic_sessions WHERE user_id = ?)
                """, userId);
        jdbc.update("DELETE FROM diagnostic_sessions WHERE user_id = ?", userId);
    }

    public void deletePracticeSessionsAndAttempts(UUID userId) {
        jdbc.update("DELETE FROM practice_sessions WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM practice_attempts WHERE user_id = ?", userId);
    }

    public void deleteStudyHeartbeatReceipts(UUID userId) {
        jdbc.update("""
                DELETE FROM study_heartbeat_receipts
                WHERE session_id IN (SELECT id FROM study_sessions WHERE user_id = ?)
                """, userId);
        jdbc.update("DELETE FROM study_sessions WHERE user_id = ?", userId);
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
