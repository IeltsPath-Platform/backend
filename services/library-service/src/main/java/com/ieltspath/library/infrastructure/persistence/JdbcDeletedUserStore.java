package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.application.port.DeletedUserStore;
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

    public record MediaRef(UUID id, String key) { }

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

    public List<MediaRef> findFlashcardImagesBatch(UUID userId, int limit) {
        return jdbc.query("""
                SELECT id, image_key
                FROM flashcards
                WHERE user_id = ? AND image_key IS NOT NULL
                ORDER BY id ASC
                LIMIT ?
                """, (rs, rowNum) -> new MediaRef(
                UUID.fromString(rs.getString("id")),
                rs.getString("image_key")), userId, limit);
    }

    public void clearFlashcardImageKeys(List<UUID> ids) {
        for (UUID id : ids) {
            jdbc.update("UPDATE flashcards SET image_key = NULL WHERE id = ?", id);
        }
    }

    public List<MediaRef> findDeckCoverImagesBatch(UUID userId, int limit) {
        return jdbc.query("""
                SELECT id, cover_image_key
                FROM flashcard_decks
                WHERE user_id = ? AND cover_image_key IS NOT NULL
                ORDER BY id ASC
                LIMIT ?
                """, (rs, rowNum) -> new MediaRef(
                UUID.fromString(rs.getString("id")),
                rs.getString("cover_image_key")), userId, limit);
    }

    public void clearDeckCoverImageKeys(List<UUID> ids) {
        for (UUID id : ids) {
            jdbc.update("UPDATE flashcard_decks SET cover_image_key = NULL WHERE id = ?", id);
        }
    }

    public void deletePersonalLibraryData(UUID userId) {
        jdbc.update("DELETE FROM outbox_events WHERE payload->>'userId' = ?", userId.toString());
        jdbc.update("""
                DELETE FROM flashcard_deck_items
                WHERE deck_id IN (SELECT id FROM flashcard_decks WHERE user_id = ?)
                   OR flashcard_id IN (SELECT id FROM flashcards WHERE user_id = ?)
                """, userId, userId);
        jdbc.update("DELETE FROM flashcard_review_events WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM flashcards WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM flashcard_decks WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM notes WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM saved_video_segments WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM video_learning_progress WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM video_dictation_attempts WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM vocabulary_weekly_goals WHERE user_id = ?", userId);
        jdbc.update("""
                DELETE FROM vocabulary_study_attempt_steps
                WHERE attempt_id IN (SELECT id FROM vocabulary_study_attempts WHERE user_id = ?)
                """, userId);
        jdbc.update("DELETE FROM vocabulary_study_submissions WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM vocabulary_study_attempts WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM vocabulary_study_start_requests WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM vocabulary_study_unit_progress WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM vocabulary_study_xp_ledger WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM vocabulary_study_streaks WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM vocabulary_study_purchases WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM vocabulary_study_profiles WHERE user_id = ?", userId);
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
