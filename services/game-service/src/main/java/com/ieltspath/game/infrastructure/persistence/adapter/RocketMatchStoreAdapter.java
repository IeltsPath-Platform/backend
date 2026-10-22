package com.ieltspath.game.infrastructure.persistence.adapter;

import com.ieltspath.game.application.port.RocketMatchStore;
import com.ieltspath.game.domain.vo.RocketEndReason;
import com.ieltspath.game.domain.vo.RocketPlayerProgress;
import com.ieltspath.game.domain.vo.RocketRoomSettings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RocketMatchStoreAdapter implements RocketMatchStore {
    private static final String MATCH = """
        SELECT id, room_id, status, end_reason, started_at, ended_at, player_mode, initial_lives, question_count,
               seconds_per_question
        FROM game_matches WHERE id=? AND game_type='ROCKET' AND NOT is_delete""";
    private static final String PLAYERS = """
        SELECT id, user_id, status, lives_remaining, score, words_processed, words_solved, total_solve_time_ms, finished_at
        FROM game_match_players WHERE match_id=? AND NOT is_delete ORDER BY joined_at, id""";

    private final JdbcTemplate sql;

    public RocketMatchStoreAdapter(JdbcTemplate sql) {
        this.sql = sql;
    }

    @Override
    public Instant now() {
        return sql.queryForObject("SELECT clock_timestamp()", Timestamp.class).toInstant();
    }

    @Override
    public Optional<State> lock(UUID matchId) {
        return load(matchId, " FOR UPDATE");
    }

    @Override
    public Optional<State> find(UUID matchId) {
        return load(matchId, "");
    }

    private record MatchRow(UUID roomId, String status, String endReason, Instant startedAt, Instant endedAt,
            RocketRoomSettings settings) {}

    /** Match row first, then its players, under the same lock clause, so the locking order stays match then players. */
    private Optional<State> load(UUID matchId, String lock) {
        Optional<MatchRow> match = sql.query(MATCH + lock, (rs, n) -> new MatchRow(rs.getObject("room_id", UUID.class),
                rs.getString("status"), rs.getString("end_reason"), instant(rs, "started_at"), instant(rs, "ended_at"),
                new RocketRoomSettings("SOLO".equals(rs.getString("player_mode")), rs.getInt("question_count"),
                        rs.getInt("initial_lives"), rs.getInt("seconds_per_question"))), matchId).stream().findFirst();
        return match.map(row -> new State(matchId, row.roomId(), row.settings(), row.status(),
                row.endReason() == null ? null : RocketEndReason.valueOf(row.endReason()), row.startedAt(), row.endedAt(),
                sql.query(PLAYERS + lock, (rs, n) -> new Player(rs.getObject("id", UUID.class),
                        rs.getObject("user_id", UUID.class), rs.getString("status"),
                        new RocketPlayerProgress(rs.getInt("lives_remaining"), rs.getLong("score"),
                                rs.getInt("words_processed"), rs.getInt("words_solved"), rs.getLong("total_solve_time_ms")),
                        instant(rs, "finished_at")), matchId)));
    }

    @Override
    public void savePlayers(List<Player> players) {
        sql.batchUpdate("""
            UPDATE game_match_players SET status=?, lives_remaining=?, score=?, words_processed=?, words_solved=?,
                total_solve_time_ms=?, finished_at=?,
                disconnected_at=CASE WHEN status='DISCONNECTED' AND ?='DISCONNECTED' THEN disconnected_at END,
                reconnect_deadline_at=CASE WHEN status='DISCONNECTED' AND ?='DISCONNECTED' THEN reconnect_deadline_at END
            WHERE id=?
            """, players, players.size(), (ps, player) -> {
                var progress = player.progress();
                ps.setString(1, player.status());
                ps.setInt(2, progress.livesRemaining());
                ps.setLong(3, progress.totalScore());
                ps.setInt(4, progress.roundsPlayed());
                ps.setInt(5, progress.correctCount());
                ps.setLong(6, progress.correctTimeMs());
                ps.setTimestamp(7, player.finishedAt() == null ? null : Timestamp.from(player.finishedAt()));
                ps.setString(8, player.status());
                ps.setString(9, player.status());
                ps.setObject(10, player.id());
            });
    }

    @Override
    public void finish(UUID matchId, UUID roomId, RocketEndReason reason, Instant at) {
        var time = Timestamp.from(at);
        sql.update("UPDATE game_matches SET status='COMPLETED', end_reason=?, ended_at=? WHERE id=? AND status='IN_PROGRESS'",
                reason.name(), time, matchId);
        rank(matchId);
        if (roomId == null) return;
        sql.update("UPDATE game_rooms SET status='WAITING', updated_at=? WHERE id=? AND status='IN_MATCH' AND NOT is_delete",
                time, roomId);
        sql.update("""
            UPDATE game_room_members SET status='JOINED', ready_at=NULL
            WHERE room_id=? AND NOT is_delete AND status IN ('READY','JOINED')
            """, roomId);
    }

    @Override
    public void finishAndCloseRoom(UUID matchId, UUID roomId, RocketEndReason reason, Instant at) {
        var time = Timestamp.from(at);
        sql.update("UPDATE game_matches SET status='COMPLETED', end_reason=?, ended_at=? WHERE id=? AND status='IN_PROGRESS'",
                reason.name(), time, matchId);
        rank(matchId);
        if (roomId == null) return;
        sql.update("UPDATE game_rooms SET status='CLOSED', is_delete=true, deleted_at=?, updated_at=? WHERE id=? AND NOT is_delete",
                time, time, roomId);
        sql.update("""
            UPDATE game_room_members SET status='LEFT', left_at=?, ready_at=NULL
            WHERE room_id=? AND NOT is_delete AND status IN ('JOINED','READY','DISCONNECTED')
            """, time, roomId);
    }

    /** Final standings, by the ranking rule: score, then less time spent on correct answers, then who joined first. */
    private void rank(UUID matchId) {
        sql.update("""
            WITH ranked AS (SELECT id, row_number() OVER (
                ORDER BY score DESC, total_solve_time_ms ASC, joined_at ASC, id ASC) AS position
                FROM game_match_players WHERE match_id=? AND NOT is_delete)
            UPDATE game_match_players p SET rank=ranked.position FROM ranked WHERE p.id=ranked.id
            """, matchId);
    }

    @Override
    public List<UUID> dueMatches(Instant now, int limit) {
        var at = Timestamp.from(now);
        return sql.queryForList("""
            SELECT r.match_id FROM game_rocket_rounds r
            JOIN game_matches m ON m.id=r.match_id AND m.status='IN_PROGRESS' AND NOT m.is_delete
            WHERE NOT r.is_delete AND ((r.status='OPEN' AND r.deadline_at<=?) OR (r.status='PENDING' AND r.opens_at<=?))
            UNION
            SELECT p.match_id FROM game_match_players p
            JOIN game_matches m ON m.id=p.match_id AND m.status='IN_PROGRESS' AND m.game_type='ROCKET' AND NOT m.is_delete
            WHERE NOT p.is_delete AND p.status='DISCONNECTED' AND p.reconnect_deadline_at<=?
            LIMIT ?
            """, UUID.class, at, at, at, limit);
    }

    @Override
    public void leaveRoom(UUID roomId, UUID userId, Instant at) {
        sql.update("UPDATE game_room_members SET status='LEFT', left_at=?, ready_at=NULL WHERE room_id=? AND user_id=? AND NOT is_delete",
                Timestamp.from(at), roomId, userId);
    }

    @Override
    public Optional<String> closeIfHostLeft(UUID roomId, Instant at) {
        Optional<String> host = sql.query("""
            SELECT COALESCE(rm.display_name, r.host_user_id::text) AS name FROM game_rooms r
            JOIN game_room_members rm ON rm.room_id=r.id AND rm.user_id=r.host_user_id
            WHERE r.id=? AND rm.status='LEFT' AND NOT r.is_delete
            """, (rs, n) -> rs.getString("name"), roomId).stream().findFirst();
        if (host.isEmpty()) return Optional.empty();
        var time = Timestamp.from(at);
        sql.update("UPDATE game_rooms SET status='CLOSED', is_delete=true, deleted_at=?, updated_at=? WHERE id=?", time, time, roomId);
        sql.update("UPDATE game_room_members SET status='LEFT', left_at=COALESCE(left_at,?), ready_at=NULL WHERE room_id=? AND NOT is_delete",
                time, roomId);
        return Optional.of("Chủ phòng " + host.get() + " đã đóng phòng");
    }

    @Override
    public String displayName(UUID roomId, UUID userId) {
        return sql.query("SELECT display_name FROM game_room_members WHERE room_id=? AND user_id=? AND display_name IS NOT NULL",
                (rs, n) -> rs.getString(1), roomId, userId).stream().findFirst().orElse(userId.toString());
    }

    @Override
    public boolean disconnect(UUID matchId, UUID userId, Instant at, Instant deadline) {
        return sql.update("""
            UPDATE game_match_players SET status='DISCONNECTED', disconnected_at=?, reconnect_deadline_at=?
            WHERE match_id=? AND user_id=? AND status='ACTIVE' AND NOT is_delete
            """, Timestamp.from(at), Timestamp.from(deadline), matchId, userId) > 0;
    }

    @Override
    public boolean reconnect(UUID matchId, UUID userId, Instant at) {
        return sql.update("""
            UPDATE game_match_players SET status='ACTIVE', disconnected_at=NULL, reconnect_deadline_at=NULL
            WHERE match_id=? AND user_id=? AND status='DISCONNECTED' AND reconnect_deadline_at>? AND NOT is_delete
            """, matchId, userId, Timestamp.from(at)) > 0;
    }

    @Override
    public List<UUID> expiredDisconnects(UUID matchId, Instant at) {
        return sql.queryForList("""
            SELECT user_id FROM game_match_players
            WHERE match_id=? AND status='DISCONNECTED' AND reconnect_deadline_at<=? AND NOT is_delete ORDER BY joined_at, id
            """, UUID.class, matchId, Timestamp.from(at));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
