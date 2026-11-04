package com.ieltspath.game.infrastructure.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Socket sessions are process-local; a restart grants persisted players time to reconnect. Lobby members get the same
 * grace; those still connected to another instance restore their seat at the next heartbeat.
 */
@Component
public class WordlyConnectionRecovery {
    private final JdbcTemplate sql;

    public WordlyConnectionRecovery(JdbcTemplate sql) { this.sql=sql; }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recover() {
        sql.update("""
            UPDATE game_match_players p SET status='DISCONNECTED',disconnected_at=statement_timestamp(),
                reconnect_deadline_at=statement_timestamp()+interval '30 seconds'
            FROM game_matches m WHERE p.match_id=m.id AND m.game_type IN ('WORDLY','ROCKET')
                AND m.player_mode='MULTIPLAYER' AND m.status='IN_PROGRESS' AND p.status='ACTIVE'
                AND NOT m.is_delete AND NOT p.is_delete
            """);
        sql.update("""
            UPDATE game_room_members rm SET status='DISCONNECTED',ready_at=NULL,
                reconnect_deadline_at=statement_timestamp()+interval '30 seconds'
            FROM game_rooms r WHERE rm.room_id=r.id AND r.game_type IN ('WORDLY','ROCKET') AND r.player_mode='MULTIPLAYER'
                AND r.status='WAITING' AND rm.status IN ('JOINED','READY') AND NOT r.is_delete AND NOT rm.is_delete
            """);
    }
}
