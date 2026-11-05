package com.ieltspath.game.infrastructure.persistence.adapter;

import com.ieltspath.game.application.port.LobbySeatStore;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LobbySeatStoreAdapter implements LobbySeatStore {
    private final JdbcTemplate sql;

    public LobbySeatStoreAdapter(JdbcTemplate sql) {
        this.sql = sql;
    }

    @Override
    public boolean lockRoom(UUID roomId) {
        return !sql.queryForList("SELECT id FROM game_rooms WHERE id=? FOR UPDATE", UUID.class, roomId).isEmpty();
    }

    @Override
    public boolean disconnect(UUID roomId, UUID userId, Instant deadline) {
        return sql.update("""
            UPDATE game_room_members rm SET status='DISCONNECTED',ready_at=NULL,reconnect_deadline_at=?
            FROM game_rooms r WHERE rm.room_id=r.id AND r.id=? AND rm.user_id=? AND r.status='WAITING'
                AND r.player_mode='MULTIPLAYER' AND NOT r.is_delete AND NOT rm.is_delete AND rm.status IN ('JOINED','READY')
            """, Timestamp.from(deadline), roomId, userId) > 0;
    }

    @Override
    public boolean reconnect(UUID roomId, UUID userId, Instant now) {
        return sql.update("""
            UPDATE game_room_members SET status='JOINED',reconnect_deadline_at=NULL
            WHERE room_id=? AND user_id=? AND status='DISCONNECTED' AND reconnect_deadline_at>? AND NOT is_delete
            """, roomId, userId, Timestamp.from(now)) > 0;
    }

    @Override
    public boolean held(UUID roomId, UUID userId, Instant now) {
        return Boolean.TRUE.equals(sql.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM game_room_members WHERE room_id=? AND user_id=? AND status='DISCONNECTED'
                AND reconnect_deadline_at>? AND NOT is_delete)
            """, Boolean.class, roomId, userId, Timestamp.from(now)));
    }

    @Override
    public boolean expired(UUID roomId, UUID userId, Instant now) {
        return Boolean.TRUE.equals(sql.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM game_room_members WHERE room_id=? AND user_id=? AND status='DISCONNECTED'
                AND reconnect_deadline_at<=? AND NOT is_delete)
            """, Boolean.class, roomId, userId, Timestamp.from(now)));
    }

    @Override
    public List<Seat> due(Instant now, int limit) {
        return sql.query("""
            SELECT rm.room_id,rm.user_id FROM game_room_members rm JOIN game_rooms r ON r.id=rm.room_id
            WHERE rm.status='DISCONNECTED' AND rm.reconnect_deadline_at<=? AND NOT rm.is_delete
                AND r.status='WAITING' AND NOT r.is_delete
            ORDER BY rm.reconnect_deadline_at,rm.id LIMIT ?
            """, (rs, n) -> new Seat(rs.getObject("room_id", UUID.class), rs.getObject("user_id", UUID.class)),
                Timestamp.from(now), limit);
    }

    @Override
    public List<UUID> dueRoomsOf(UUID userId, Instant now) {
        return sql.queryForList("""
            SELECT rm.room_id FROM game_room_members rm JOIN game_rooms r ON r.id=rm.room_id
            WHERE rm.user_id=? AND rm.status='DISCONNECTED' AND rm.reconnect_deadline_at<=? AND NOT rm.is_delete
                AND r.status='WAITING' AND NOT r.is_delete
            """, UUID.class, userId, Timestamp.from(now));
    }
}
