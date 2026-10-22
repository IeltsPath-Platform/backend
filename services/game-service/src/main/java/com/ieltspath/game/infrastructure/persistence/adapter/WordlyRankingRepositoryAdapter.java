package com.ieltspath.game.infrastructure.persistence.adapter;

import com.ieltspath.game.application.port.WordlyRankingRepository;
import com.ieltspath.game.application.result.WordlyRankingResult;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Ranking of a Wordly or Rocket match; both keep score, correct count and solve time in the same player columns. */
@Repository
public class WordlyRankingRepositoryAdapter implements WordlyRankingRepository {
    private final JdbcTemplate sql;

    public WordlyRankingRepositoryAdapter(JdbcTemplate sql) {
        this.sql = sql;
    }

    @Override
    public Optional<WordlyRankingResult> find(UUID matchId, UUID userId) {
        var rows = sql.query("""
                SELECT m.status AS match_status, m.player_mode, CURRENT_TIMESTAMP AS server_time,
                       p.user_id, COALESCE(p.player_display_name, rm.display_name) AS player_display_name,
                       COALESCE(p.player_slot_number, rm.slot_number) AS player_slot_number, p.status,
                       p.lives_remaining, p.score, p.total_solve_time_ms,
                       p.words_processed, p.words_solved, p.current_streak,
                       CASE WHEN m.status = 'IN_PROGRESS' AND p.status = 'DISCONNECTED'
                           THEN p.reconnect_deadline_at END AS reconnect_deadline_at,
                       CASE WHEN m.status = 'COMPLETED' THEN p.rank ELSE
                           CAST(ROW_NUMBER() OVER (ORDER BY p.score DESC,
                               p.total_solve_time_ms ASC, p.score_reached_at ASC NULLS LAST,
                               p.joined_at ASC, p.id ASC) AS INTEGER) END AS standing
                FROM game_matches m JOIN game_match_players p ON p.match_id = m.id
                LEFT JOIN game_room_members rm ON rm.id = p.room_member_id
                WHERE m.id = ? AND m.game_type IN ('WORDLY', 'ROCKET') AND NOT m.is_delete AND NOT p.is_delete
                  AND EXISTS (SELECT 1 FROM game_match_players caller
                              WHERE caller.match_id = m.id AND caller.user_id = ?
                                AND NOT caller.is_delete)
                ORDER BY standing ASC NULLS LAST, p.id ASC
                """, (r, n) -> new Row(r.getString("match_status"), r.getString("player_mode"),
                r.getTimestamp("server_time").toInstant(), new WordlyRankingResult.Player(
                    r.getObject("user_id", UUID.class), r.getString("player_display_name"),
                    r.getObject("player_slot_number", Integer.class), r.getObject("standing", Integer.class),
                    r.getString("status"), r.getInt("lives_remaining"), r.getLong("score"),
                    r.getLong("total_solve_time_ms"), r.getInt("words_processed"),
                    r.getInt("words_solved"), r.getInt("current_streak"),
                    instant(r.getTimestamp("reconnect_deadline_at")))), matchId, userId);
        if (rows.isEmpty()) return Optional.empty();
        var first = rows.getFirst();
        return Optional.of(new WordlyRankingResult(matchId, first.status(), first.playerMode(),
                "COMPLETED".equals(first.status()), first.serverTime(),
                rows.stream().map(Row::player).toList()));
    }

    private static java.time.Instant instant(java.sql.Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private record Row(String status, String playerMode, java.time.Instant serverTime,
            WordlyRankingResult.Player player) {}
}
