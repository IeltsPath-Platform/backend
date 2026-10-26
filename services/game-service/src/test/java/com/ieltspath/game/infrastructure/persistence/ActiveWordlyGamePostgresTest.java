package com.ieltspath.game.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.game.infrastructure.persistence.adapter.ActiveWordlyGameRepositoryAdapter;
import com.ieltspath.game.infrastructure.persistence.adapter.SoloGameplayRepositoryAdapter;
import com.ieltspath.game.infrastructure.persistence.adapter.WordlyRankingRepositoryAdapter;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class ActiveWordlyGamePostgresTest {
    @Container static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:15-alpine");
    JdbcTemplate sql;
    TransactionTemplate tx;
    ActiveWordlyGameRepositoryAdapter games;
    UUID user;

    @BeforeEach void setup() {
        var ds = new DriverManagerDataSource(db.getJdbcUrl(), db.getUsername(), db.getPassword());
        Flyway.configure().dataSource(ds).load().migrate();
        sql = new JdbcTemplate(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        games = new ActiveWordlyGameRepositoryAdapter(sql);
        user = UUID.randomUUID();
    }

    UUID room(String playerMode, String status, UUID host) {
        return room("WORDLY", playerMode, status, host);
    }

    UUID room(String gameType, String playerMode, String status, UUID host) {
        UUID room = UUID.randomUUID();
        sql.update("""
            INSERT INTO game_rooms(id,room_code,host_user_id,game_type,learning_domain,mode,max_players,config_snapshot,status,created_at,updated_at,player_mode,
                question_count,seconds_per_question)
            VALUES (?,upper(substring(md5(?::text),1,6)),?,?,'VOCABULARY','PRACTICE',?,'{}',?,now(),now(),?,?,?)
            """, room, room, host, gameType, "SOLO".equals(playerMode) ? 1 : 4, status, playerMode,
                "ROCKET".equals(gameType) ? 6 : null, "ROCKET".equals(gameType) ? 15 : null);
        return room;
    }

    void member(UUID room, UUID userId, String role, String status, int slot) {
        sql.update("""
            INSERT INTO game_room_members(id,room_id,user_id,member_role,status,joined_at,slot_number,display_name)
            VALUES (gen_random_uuid(),?,?,?,?,now(),?,'Player')
            """, room, userId, role, status, slot);
    }

    UUID match(UUID room, String playerMode, UUID userId, String playerStatus) {
        UUID match = UUID.randomUUID();
        sql.update("""
            INSERT INTO game_matches(id,room_id,game_type,learning_domain,config_snapshot,content_snapshot,status,started_at,created_at,player_mode,word_limit,word_time_limit_seconds)
            VALUES (?,?,'WORDLY','VOCABULARY','{}','{}','IN_PROGRESS',now(),now(),?,5,180)
            """, match, room, playerMode);
        sql.update("""
            INSERT INTO game_match_players(id,match_id,room_member_id,user_id,status,joined_at)
            SELECT gen_random_uuid(),?,id,?,?,now() FROM game_room_members WHERE room_id=? AND user_id=?
            """, match, userId, playerStatus, room, userId);
        return match;
    }

    @Test void findsAWaitingMultiplayerLobbyMember() {
        UUID room = room("MULTIPLAYER", "WAITING", user);
        member(room, user, "HOST", "JOINED", 1);
        var found = games.find(user).orElseThrow();
        assertEquals(room, found.roomId());
        assertEquals("WAITING", found.roomStatus());
        assertNull(found.matchId());
    }

    @Test void prefersTheRunningMatchAndReportsThePlayerStatus() {
        UUID lobby = room("MULTIPLAYER", "WAITING", user);
        member(lobby, user, "HOST", "JOINED", 1);
        UUID running = room("MULTIPLAYER", "IN_MATCH", UUID.randomUUID());
        member(running, user, "PLAYER", "READY", 2);
        UUID match = match(running, "MULTIPLAYER", user, "DISCONNECTED");
        var found = games.find(user).orElseThrow();
        assertEquals(match, found.matchId());
        assertEquals("DISCONNECTED", found.playerStatus());
        assertTrue(found.playing());
    }

    @Test void leftMembersAndIdleSoloRoomsAreNotActive() {
        UUID left = room("MULTIPLAYER", "WAITING", UUID.randomUUID());
        member(left, user, "PLAYER", "LEFT", 2);
        UUID solo = room("SOLO", "WAITING", user);
        member(solo, user, "HOST", "JOINED", 1);
        assertTrue(games.find(user).isEmpty());
        games.closeIdleSoloRooms(user, Instant.now());
        assertEquals("CLOSED", sql.queryForObject("SELECT status FROM game_rooms WHERE id=?", String.class, solo));
        assertEquals("LEFT", sql.queryForObject("SELECT status FROM game_room_members WHERE room_id=?", String.class, solo));
    }

    @Test void findsARunningSoloMatch() {
        UUID solo = room("SOLO", "IN_MATCH", user);
        member(solo, user, "HOST", "READY", 1);
        UUID match = match(solo, "SOLO", user, "ACTIVE");
        games.closeIdleSoloRooms(user, Instant.now());
        assertEquals(match, games.find(user).orElseThrow().matchId());
    }

    @Test void spectatorLeavesTheRoomWithoutChangingTheirMatchStatus() {
        UUID running = room("MULTIPLAYER", "IN_MATCH", UUID.randomUUID());
        member(running, user, "PLAYER", "READY", 2);
        match(running, "MULTIPLAYER", user, "ELIMINATED");
        games.leaveRunningMatchRoom(running, user, Instant.now());
        assertTrue(games.find(user).isEmpty());
        assertEquals("ELIMINATED", sql.queryForObject("SELECT status FROM game_match_players WHERE user_id=?", String.class, user));
    }

    @Test void oneRoomPerUserSpansEveryRoomGame() {
        UUID rocket = room("ROCKET", "MULTIPLAYER", "WAITING", UUID.randomUUID());
        member(rocket, user, "PLAYER", "JOINED", 2);
        var found = games.find(user).orElseThrow();
        assertEquals(rocket, found.roomId());
        assertEquals("ROCKET", found.gameType());
        UUID wordly = room("MULTIPLAYER", "WAITING", user);
        member(wordly, user, "HOST", "JOINED", 1);
        assertTrue(java.util.Set.of("WORDLY", "ROCKET").contains(games.find(user).orElseThrow().gameType()));
    }

    @Test void legacySessionRoomsDoNotCountAsActive() {
        UUID legacy = room("WORD_MEANING_MATCH", "MULTIPLAYER", "WAITING", user);
        member(legacy, user, "HOST", "JOINED", 1);
        assertTrue(games.find(user).isEmpty());
    }

    @Test void idleSoloRoomsOfAnyRoomGameAreClosed() {
        UUID rocketSolo = room("ROCKET", "SOLO", "WAITING", user);
        member(rocketSolo, user, "HOST", "JOINED", 1);
        games.closeIdleSoloRooms(user, Instant.now());
        assertEquals("CLOSED", sql.queryForObject("SELECT status FROM game_rooms WHERE id=?", String.class, rocketSolo));
        assertEquals("WORDLY", new com.ieltspath.game.application.result.ActiveWordlyGameResult(
                rocketSolo, "ABC123", "SOLO", "WAITING", null, null).gameType());
    }

    @Test void userLockIsTransactionScoped() {
        tx.executeWithoutResult(t -> games.lockUser(user));
        tx.executeWithoutResult(t -> games.lockUser(user));
    }

    @Test void rankingExposesTheReconnectDeadlineOnlyDuringGrace() {
        UUID running = room("MULTIPLAYER", "IN_MATCH", user);
        member(running, user, "HOST", "READY", 1);
        UUID match = match(running, "MULTIPLAYER", user, "ACTIVE");
        var ranking = new WordlyRankingRepositoryAdapter(sql);
        assertNull(ranking.find(match, user).orElseThrow().players().getFirst().reconnectDeadlineAt());
        Instant at = Instant.parse("2026-10-20T00:00:00Z");
        new SoloGameplayRepositoryAdapter(sql, new ObjectMapper().findAndRegisterModules()).disconnected(match, user, at);
        assertEquals(at.plusSeconds(30), ranking.find(match, user).orElseThrow().players().getFirst().reconnectDeadlineAt());
    }
}
