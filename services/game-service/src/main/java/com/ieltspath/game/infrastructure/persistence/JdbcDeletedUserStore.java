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

    public void deleteUserGameData(UUID userId) {
        jdbc.update("DELETE FROM leaderboard_entries WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM quiz_participations WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM game_guess_receipts WHERE user_id = ?", userId);
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
    }

    public void markJobDone(UUID eventId) {
        jdbc.update("""
                UPDATE user_deletion_jobs
                SET status = 'DONE', completed_at = CURRENT_TIMESTAMP
                WHERE event_id = ?
                """, eventId);
    }
}
