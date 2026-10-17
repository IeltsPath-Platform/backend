package com.ieltspath.game.infrastructure.persistence;

import com.ieltspath.game.infrastructure.persistence.adapter.WordlyRankingRepositoryAdapter;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class WordlyRankingPostgresTest {
    @Container static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test void ranksByScoreThenSolveTimeThenScoreReachedTimeAndUsesFinalPersistedRanks() {
        var ds = new DriverManagerDataSource(db.getJdbcUrl(), db.getUsername(), db.getPassword());
        Flyway.configure().dataSource(ds).load().migrate();
        var sql = new JdbcTemplate(ds);
        var rankings = new WordlyRankingRepositoryAdapter(sql);
        UUID match = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
        sql.update("""
                INSERT INTO game_matches(id,game_type,learning_domain,config_snapshot,content_snapshot,status,created_at)
                VALUES (?,'WORDLY','VOCABULARY','{}','{}','IN_PROGRESS',now())
                """, match);
        addPlayer(sql, match, first, 4, "FORFEITED", "2026-10-17T01:00:00Z");
        addPlayer(sql, match, second, 1, "ACTIVE", "2026-10-17T02:00:00Z");
        var result = rankings.find(match, first).orElseThrow();
        assertFalse(result.isFinal());
        assertNotNull(result.serverTime());
        assertEquals(first, result.players().getFirst().userId());
        assertEquals(1, result.players().getFirst().rank());
        assertEquals(4, result.players().getFirst().slotNumber());
        assertEquals("Snapshot name", result.players().getFirst().displayName());
        assertTrue(rankings.find(match, UUID.randomUUID()).isEmpty());
        sql.update("UPDATE game_match_players SET total_solve_time_ms=1 WHERE user_id=?", second);
        assertEquals(second, rankings.find(match, first).orElseThrow().players().getFirst().userId());
        sql.update("UPDATE game_match_players SET score=101 WHERE user_id=?", first);
        assertEquals(first, rankings.find(match, first).orElseThrow().players().getFirst().userId());
        sql.update("UPDATE game_match_players SET rank=CASE WHEN user_id=? THEN 2 ELSE 1 END WHERE match_id=?", first, match);
        sql.update("UPDATE game_matches SET status='COMPLETED' WHERE id=?", match);
        var finalResult = rankings.find(match, first).orElseThrow();
        assertTrue(finalResult.isFinal());
        assertEquals(second, finalResult.players().getFirst().userId());
        assertEquals(2, finalResult.players().getLast().rank());
        sql.update("UPDATE game_match_players SET is_delete=true,deleted_at=now() WHERE user_id=?", second);
        assertEquals(1, rankings.find(match, first).orElseThrow().players().size());
        assertTrue(rankings.find(match, second).isEmpty());
        sql.update("UPDATE game_matches SET game_type='QUIZ' WHERE id=?", match);
        assertTrue(rankings.find(match, first).isEmpty());
        sql.update("UPDATE game_matches SET game_type='WORDLY',is_delete=true,deleted_at=now() WHERE id=?", match);
        assertTrue(rankings.find(match, first).isEmpty());
    }

    private void addPlayer(JdbcTemplate sql, UUID match, UUID user, int slot, String status, String reached) {
        sql.update("""
                INSERT INTO game_match_players(id,match_id,user_id,status,joined_at,score,
                  total_solve_time_ms,score_reached_at,player_display_name,player_slot_number)
                VALUES (gen_random_uuid(),?,?,?,now(),100,1000,CAST(? AS TIMESTAMPTZ),'Snapshot name',?)
                """, match, user, status, reached, slot);
    }
}
