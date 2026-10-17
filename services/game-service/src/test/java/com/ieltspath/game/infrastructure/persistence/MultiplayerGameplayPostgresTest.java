package com.ieltspath.game.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.game.application.port.WordlyWordProvider;
import com.ieltspath.game.application.result.*;
import com.ieltspath.game.application.usecase.SoloGameplayUseCase;
import com.ieltspath.game.infrastructure.persistence.adapter.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Testcontainers(disabledWithoutDocker = true)
class MultiplayerGameplayPostgresTest {
    @Container static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:15-alpine");
    JdbcTemplate sql;
    SoloGameplayRepositoryAdapter store;
    WordlyMatchStoreAdapter words;
    SoloGameplayUseCase game;
    UUID match, room;
    List<UUID> users, players;
    List<Object> events;
    static final List<String> ANSWERS = List.of("AIR", "BOOK", "ACTOR", "PLANET", "WATER");

    @BeforeEach void setup() {
        var ds = new DriverManagerDataSource(db.getJdbcUrl(), db.getUsername(), db.getPassword());
        Flyway.configure().dataSource(ds).load().migrate();
        sql = new JdbcTemplate(ds);
        store = new SoloGameplayRepositoryAdapter(sql, new ObjectMapper().findAndRegisterModules());
        words = new WordlyMatchStoreAdapter(sql);
        events = new CopyOnWriteArrayList<>();
        game = new SoloGameplayUseCase(store, words, new DataSourceTransactionManager(ds), events::add);
        match = UUID.randomUUID(); room = UUID.randomUUID();
        users = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        players = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        sql.update("""
            INSERT INTO game_rooms(id,room_code,host_user_id,game_type,learning_domain,mode,max_players,config_snapshot,status,created_at,updated_at,player_mode)
            VALUES (?,upper(substring(md5(?::text),1,6)),?,'WORDLY','VOCABULARY','PRACTICE',4,'{}','IN_MATCH',now(),now(),'MULTIPLAYER')
            """, room, room, users.get(0));
        for (int i=0;i<3;i++) sql.update("""
            INSERT INTO game_room_members(id,room_id,user_id,member_role,status,joined_at,slot_number,display_name)
            VALUES (gen_random_uuid(),?,?,?,'READY',now(),?,?)
            """, room, users.get(i), i==0 ? "HOST" : "PLAYER", i+1, "Player " + i);
        sql.update("""
            INSERT INTO game_matches(id,room_id,game_type,learning_domain,config_snapshot,content_snapshot,status,started_at,created_at,player_mode,word_limit,word_time_limit_seconds)
            VALUES (?,?,'WORDLY','VOCABULARY','{}','{}','IN_PROGRESS',now(),now(),'MULTIPLAYER',5,180)
            """, match, room);
        for(int i=0;i<3;i++) sql.update("""
            INSERT INTO game_match_players(id,match_id,room_member_id,user_id,status,joined_at)
            SELECT ?,?,id,?,'ACTIVE',now() FROM game_room_members WHERE room_id=? AND user_id=?
            """, players.get(i),match,users.get(i),room,users.get(i));
        var pool = ANSWERS.stream().map(a -> new WordlyWordProvider.Word(UUID.randomUUID(),a,"Meaning " + a)).toList();
        words.initialize(match,pool,players,Instant.now(),true);
        words.preparePool(match,pool);
    }
    CurrentWordResult current(int i) { return words.current(match,users.get(i)).orElseThrow(); }
    SoloReply solve(int i) {
        var w=current(i);
        return game.submit(match,w.playerWordId(),users.get(i),UUID.randomUUID(),ANSWERS.get(w.wordNumber()-1));
    }
    String matchStatus() { return sql.queryForObject("SELECT status FROM game_matches WHERE id=?",String.class,match); }
    String playerStatus(int i) { return sql.queryForObject("SELECT status FROM game_match_players WHERE id=?",String.class,players.get(i)); }
    long score(int i) { return sql.queryForObject("SELECT score FROM game_match_players WHERE id=?",Long.class,players.get(i)); }
    void finish(int i) { for(int n=0;n<5;n++) assertEquals(200,solve(i).status()); }

    @Test void playersAdvanceIndependentlyThroughTheSharedSequence() {
        assertNotEquals(current(0).playerWordId(),current(1).playerWordId());
        assertEquals(200,solve(0).status());
        assertEquals(2,current(0).wordNumber());
        assertEquals(1,current(1).wordNumber());
        assertEquals(0,score(1));
        assertEquals(200,solve(1).status());
        assertEquals(current(0).meaning(),current(1).meaning());
        assertEquals("IN_PROGRESS",matchStatus());
    }
    @Test void processingOneUsersRequestAlsoSettlesOtherUsersPendingReceipt() {
        UUID request=UUID.randomUUID();
        store.receive(match,current(1).playerWordId(),users.get(1),request,"AIR");
        assertEquals(200,solve(0).status());
        assertEquals(200,store.receipt(request).reply().status());
        assertEquals(store.receipt(request).received(),
                sql.queryForObject("SELECT score_reached_at FROM game_match_players WHERE id=?",
                        java.sql.Timestamp.class,players.get(1)).toInstant());
        assertEquals(2,current(1).wordNumber());
        assertEquals(1,current(2).wordNumber());
        assertTrue(score(1)>0);
    }
    @Test void concurrentRetriesAndDistinctRequestsCannotScoreTwice() throws Exception {
        var executor=Executors.newFixedThreadPool(2);
        try {
            var w=current(0); UUID request=UUID.randomUUID();
            var a=executor.submit(() -> game.submit(match,w.playerWordId(),users.get(0),request,"AIR"));
            var b=executor.submit(() -> game.submit(match,w.playerWordId(),users.get(0),request,"AIR"));
            assertEquals(a.get(20,TimeUnit.SECONDS).body(),b.get(20,TimeUnit.SECONDS).body());
            assertEquals(1,sql.queryForObject("SELECT words_solved FROM game_match_players WHERE id=?",Integer.class,players.get(0)));
            var next=current(0);
            var c=executor.submit(() -> game.submit(match,next.playerWordId(),users.get(0),UUID.randomUUID(),"BOOK"));
            var d=executor.submit(() -> game.submit(match,next.playerWordId(),users.get(0),UUID.randomUUID(),"BOOK"));
            assertEquals(Set.of(200,409),Set.of(c.get(20,TimeUnit.SECONDS).status(),d.get(20,TimeUnit.SECONDS).status()));
            assertEquals(2,sql.queryForObject("SELECT words_solved FROM game_match_players WHERE id=?",Integer.class,players.get(0)));
        } finally { executor.shutdownNow(); }
    }
    @Test void hostLeavingContinuesUntilNormalFinishThenClosesAndRetainsResults() {
        var w=current(0); solve(0); long saved=score(0);
        game.leave(match,users.get(0));
        assertEquals("FORFEITED",playerStatus(0));
        var liveRanking = new WordlyRankingRepositoryAdapter(sql).find(match,users.get(0)).orElseThrow();
        assertFalse(liveRanking.isFinal());
        assertEquals(3,liveRanking.players().size());
        assertEquals("FORFEITED",liveRanking.players().stream()
                .filter(p -> p.userId().equals(users.get(0))).findFirst().orElseThrow().status());
        assertEquals("IN_PROGRESS",matchStatus());
        assertEquals("IN_MATCH",sql.queryForObject("SELECT status FROM game_rooms WHERE id=?",String.class,room));
        assertEquals(409,game.submit(match,w.playerWordId(),users.get(0),UUID.randomUUID(),"AIR").status());
        finish(1);
        assertEquals("IN_PROGRESS",matchStatus());
        finish(2);
        assertEquals("COMPLETED",matchStatus());
        assertEquals("CLOSED",sql.queryForObject("SELECT status FROM game_rooms WHERE id=?",String.class,room));
        var result=game.result(match,users.get(0));
        var ranking=(List<?>)result.get("players");
        assertEquals(3,ranking.size());
        assertEquals(saved,score(0));
        assertEquals("FORFEITED",((Map<?,?>)result.get("player")).get("status"));
        var update=events.stream().filter(WordlyMatchUpdatedEvent.class::isInstance).map(WordlyMatchUpdatedEvent.class::cast).reduce((a,b)->b).orElseThrow();
        assertTrue(update.finished());
        assertTrue(update.closureMessage().contains("Player 0"));
        assertTrue(events.stream().filter(SoloGameplayEvent.class::isInstance)
                .map(SoloGameplayEvent.class::cast)
                .noneMatch(e -> "MATCH_FINISHED".equals(e.payload().get("type"))),
                "Multiplayer completion must use the public final event, without a duplicate private final event");
    }
    @Test void finishedPlayersWithLivesDoNotPrematurelyEndTheMatch() {
        finish(0); finish(1);
        assertEquals("FINISHED",playerStatus(0));
        assertEquals("IN_PROGRESS",matchStatus());
        assertEquals(1,current(2).wordNumber());
        finish(2);
        assertEquals("COMPLETED",matchStatus());
        assertEquals("WAITING",sql.queryForObject("SELECT status FROM game_rooms WHERE id=?",String.class,room));
    }
    @Test void timeoutConsumesExactlyOneLifeAndDoesNotAdvanceOpponents() {
        var w=current(1);
        sql.update("UPDATE game_player_words SET started_at=now()-interval '181 seconds',deadline_at=now()-interval '1 second' WHERE id=?",w.playerWordId());
        game.timeout(match,users.get(1)); game.timeout(match,users.get(1));
        assertEquals(3,current(1).livesRemaining());
        assertEquals(2,current(1).wordNumber());
        assertEquals(4,current(0).livesRemaining());
        assertEquals(1,current(0).wordNumber());
    }
    @Test void reconnectPreservesWordDeadlineAndCannotReviveAfterGrace() {
        var before=current(1);
        game.disconnected(match,users.get(1));
        assertEquals("DISCONNECTED",playerStatus(1));
        game.reconnect(match,users.get(1));
        assertEquals("ACTIVE",playerStatus(1));
        assertEquals(before.deadlineAt(),current(1).deadlineAt());
        game.disconnected(match,users.get(1));
        sql.update("UPDATE game_match_players SET disconnected_at=now()-interval '31 seconds',reconnect_deadline_at=now()-interval '1 second' WHERE id=?",players.get(1));
        game.reconnect(match,users.get(1));
        assertEquals("FORFEITED",playerStatus(1));
        assertTrue(words.current(match,users.get(1)).isEmpty());
        game.reconnect(match,users.get(1));
        assertEquals("FORFEITED",playerStatus(1));
        assertEquals("IN_PROGRESS",matchStatus());
    }
    @Test void anotherPlayersWordCannotBeSubmittedAndOutsidersCannotReadBoards() {
        var target=current(1);
        assertThrows(com.ieltspath.game.domain.exception.GameMatchNotFoundException.class, () -> game.submit(match,target.playerWordId(),users.get(0),UUID.randomUUID(),"AIR"));
        assertEquals(0,current(1).attemptsUsed());
        assertEquals(0,score(0));
        assertTrue(words.current(match,UUID.randomUUID()).isEmpty());
        assertThrows(com.ieltspath.game.domain.exception.GameMatchNotFoundException.class,()->game.result(match,UUID.randomUUID()));
    }
    @Test void rankingUsesScoreThenSolveTimeThenEarlierScoreReachedAt() {
        sql.update("UPDATE game_match_players SET score=100,total_solve_time_ms=1000,score_reached_at=now()-interval '2 seconds' WHERE id=?",players.get(0));
        sql.update("UPDATE game_match_players SET score=100,total_solve_time_ms=1000,score_reached_at=now()-interval '3 seconds' WHERE id=?",players.get(1));
        sql.update("UPDATE game_match_players SET score=100,total_solve_time_ms=2000,score_reached_at=now()-interval '4 seconds' WHERE id=?",players.get(2));
        game.leave(match,users.get(2)); game.leave(match,users.get(0));
        var ranks=sql.queryForList("SELECT user_id FROM game_match_players WHERE match_id=? ORDER BY rank",UUID.class,match);
        assertEquals(List.of(users.get(1),users.get(0),users.get(2)),ranks);
    }
}
