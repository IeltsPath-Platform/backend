package com.ieltspath.game.infrastructure.persistence.adapter;

import com.ieltspath.game.application.port.ActiveWordlyGameRepository;
import com.ieltspath.game.application.result.ActiveWordlyGameResult;
import com.ieltspath.game.domain.vo.RoomGame;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ActiveWordlyGameRepositoryAdapter implements ActiveWordlyGameRepository {
    /** SQL list of every room game, e.g. 'WORDLY','ROCKET'; built from the enum, never from input. */
    private static final String ROOM_GAMES = RoomGame.storedTypes().stream().map(type -> "'" + type + "'")
            .collect(java.util.stream.Collectors.joining(","));

    private final JdbcTemplate sql;

    public ActiveWordlyGameRepositoryAdapter(JdbcTemplate sql) {
        this.sql = sql;
    }

    @Override
    public void lockUser(UUID userId) {
        sql.query("SELECT pg_advisory_xact_lock(hashtextextended('room-admission:' || ?::text, 0))",
                rs -> {}, userId);
    }

    @Override
    public void closeIdleSoloRooms(UUID userId, Instant now) {
        var at = Timestamp.from(now);
        sql.update("""
            UPDATE game_room_members rm SET status='LEFT',left_at=?,ready_at=NULL
            FROM game_rooms r WHERE rm.room_id=r.id AND r.host_user_id=? AND r.game_type IN (%s)
                AND r.player_mode='SOLO' AND r.status='WAITING' AND NOT r.is_delete AND NOT rm.is_delete
                AND rm.status IN ('JOINED','READY','DISCONNECTED')
            """.formatted(ROOM_GAMES), at, userId);
        sql.update("""
            UPDATE game_rooms SET status='CLOSED',updated_at=?
            WHERE host_user_id=? AND game_type IN (%s) AND player_mode='SOLO' AND status='WAITING' AND NOT is_delete
            """.formatted(ROOM_GAMES), at, userId);
    }

    @Override
    public Optional<ActiveWordlyGameResult> find(UUID userId) {
        return sql.query("""
            SELECT r.id AS room_id, r.room_code, r.player_mode, r.status AS room_status, r.game_type,
                   m.id AS match_id, p.status AS player_status
            FROM game_rooms r
            JOIN game_room_members rm ON rm.room_id=r.id AND rm.user_id=? AND NOT rm.is_delete
                AND rm.status IN ('JOINED','READY','DISCONNECTED')
            LEFT JOIN game_matches m ON m.room_id=r.id AND m.status='IN_PROGRESS' AND NOT m.is_delete
            LEFT JOIN game_match_players p ON p.match_id=m.id AND p.user_id=rm.user_id AND NOT p.is_delete
            WHERE r.game_type IN (%s) AND NOT r.is_delete
              AND ((r.player_mode='MULTIPLAYER' AND r.status IN ('WAITING','IN_MATCH'))
                OR (r.player_mode='SOLO' AND r.status='IN_MATCH' AND m.id IS NOT NULL))
            ORDER BY (m.id IS NOT NULL) DESC, r.updated_at DESC, r.id
            LIMIT 1
            """.formatted(ROOM_GAMES), (r, n) -> new ActiveWordlyGameResult(r.getObject("room_id", UUID.class),
                r.getString("room_code"), r.getString("player_mode"), r.getString("room_status"),
                r.getObject("match_id", UUID.class), r.getString("player_status"), r.getString("game_type")),
                userId).stream().findFirst();
    }

    @Override
    public void leaveRunningMatchRoom(UUID roomId, UUID userId, Instant now) {
        sql.update("""
            UPDATE game_room_members SET status='LEFT',left_at=?,ready_at=NULL
            WHERE room_id=? AND user_id=? AND NOT is_delete AND status IN ('JOINED','READY','DISCONNECTED')
            """, Timestamp.from(now), roomId, userId);
    }
}
