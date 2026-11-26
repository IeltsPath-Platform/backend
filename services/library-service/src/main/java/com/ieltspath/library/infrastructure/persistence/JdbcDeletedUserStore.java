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

    public List<String> findFlashcardImageKeys(UUID userId) {
        return jdbc.queryForList("""
                SELECT image_key FROM flashcards
                WHERE user_id = ? AND image_key IS NOT NULL
                UNION
                SELECT cover_image_key FROM flashcard_decks
                WHERE user_id = ? AND cover_image_key IS NOT NULL
                """, String.class, userId, userId);
    }

    public void deletePersonalLibraryData(UUID userId) {
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
                SET status = 'DONE', completed_at = CURRENT_TIMESTAMP
                WHERE event_id = ?
                """, eventId);
    }
}
