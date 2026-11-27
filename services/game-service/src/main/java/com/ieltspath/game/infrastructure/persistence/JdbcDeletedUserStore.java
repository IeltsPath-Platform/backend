package com.ieltspath.game.infrastructure.persistence;

import com.ieltspath.game.application.port.DeletedUserStore;
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

    /** Every table with a user column that {@link #deleteUserGameData} handles; checked against the schema by a test. */
    public static final java.util.Set<String> HANDLED_TABLES = java.util.Set.of(
            "game_guess_receipts", "game_sessions", "game_match_players", "game_room_members", "game_rooms");

    /** Stands in for a deleted host, because a room must keep one. */
    public static final UUID DELETED_USER = new UUID(0L, 0L);

    public void deleteUserGameData(UUID userId) {
        jdbc.update("DELETE FROM game_guess_receipts WHERE user_id = ?", userId);
        // A Wordly hint names the guess it came from; the five hint columns are set or cleared together.
        jdbc.update("""
                UPDATE game_player_words
                SET hint_request_id = NULL, hint_source_guess_id = NULL, hint_column = NULL, hint_letter = NULL,
                    hint_used_at = NULL
                WHERE hint_source_guess_id IN (
                    SELECT g.id FROM game_guesses g
                    JOIN game_player_words pw ON pw.id = g.player_word_id
                    JOIN game_match_players mp ON mp.id = pw.match_player_id
                    WHERE mp.user_id = ?
                )
                """, userId);
        jdbc.update("""
                DELETE FROM game_guesses
                WHERE player_word_id IN (
                    SELECT pw.id FROM game_player_words pw
                    JOIN game_match_players mp ON mp.id = pw.match_player_id
                    WHERE mp.user_id = ?
                )
                """, userId);
        jdbc.update("""
                DELETE FROM game_player_words
                WHERE match_player_id IN (SELECT id FROM game_match_players WHERE user_id = ?)
                """, userId);
        jdbc.update("""
                DELETE FROM game_rocket_answers
                WHERE player_id IN (SELECT id FROM game_match_players WHERE user_id = ?)
                """, userId);
        jdbc.update("""
                DELETE FROM game_events
                WHERE match_player_id IN (SELECT id FROM game_match_players WHERE user_id = ?)
                """, userId);
        jdbc.update("""
                DELETE FROM game_answers
                WHERE session_id IN (SELECT id FROM game_sessions WHERE user_id = ?)
                """, userId);
        jdbc.update("DELETE FROM game_sessions WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM game_match_players WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM game_room_members WHERE user_id = ?", userId);
        jdbc.update("UPDATE game_rooms SET host_user_id = ? WHERE host_user_id = ?", DELETED_USER, userId);
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
