package com.ieltspath.game.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.game.application.port.*;
import com.ieltspath.game.application.usecase.*;
import com.ieltspath.game.infrastructure.persistence.adapter.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import java.time.Instant;
import java.util.*;

@Testcontainers(disabledWithoutDocker = true)
class SoloGameplayPostgresTest {
    @Container static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:15-alpine");
    JdbcTemplate sql;
    SoloGameplayRepositoryAdapter store;
    WordlyMatchStoreAdapter words;
    SoloGameplayUseCase game;
    UUID match, room, player, user;

    @BeforeEach
    void setup() {
        var ds = new DriverManagerDataSource(db.getJdbcUrl(), db.getUsername(), db.getPassword());
        Flyway.configure().dataSource(ds).load().migrate();
        sql = new JdbcTemplate(ds);
        store = new SoloGameplayRepositoryAdapter(sql, new ObjectMapper().findAndRegisterModules());
        words = new WordlyMatchStoreAdapter(sql);
        game = new SoloGameplayUseCase(store, words, new DataSourceTransactionManager(ds), e -> {}, java.time.Clock.systemUTC());
        match = UUID.randomUUID();
        room = UUID.randomUUID();
        player = UUID.randomUUID();
        user = UUID.randomUUID();
        sql.update(
                "INSERT INTO"
                    + " game_rooms(id,room_code,host_user_id,game_type,learning_domain,mode,max_players,config_snapshot,status,created_at,updated_at,player_mode)"
                    + " VALUES"
                    + " (?,upper(substring(md5(?::text),1,6)),?,'WORDLY','VOCABULARY','PRACTICE',1,'{}','IN_MATCH',now(),now(),'SOLO')",
                room,
                room,
                user);
        sql.update("UPDATE game_rooms SET room_code=upper(room_code) WHERE id=?", room);
        sql.update(
                "INSERT INTO"
                    + " game_room_members(id,room_id,user_id,member_role,status,joined_at,slot_number,display_name)"
                    + " VALUES (gen_random_uuid(),?,?,'HOST','READY',now(),1,'Fixture Player')",
                room,
                user);
        sql.update(
                "INSERT INTO"
                    + " game_matches(id,room_id,game_type,learning_domain,config_snapshot,content_snapshot,status,started_at,created_at,player_mode,word_limit,word_time_limit_seconds)"
                    + " VALUES"
                    + " (?,?,'WORDLY','VOCABULARY','{}','{}','IN_PROGRESS',now(),now(),'SOLO',5,NULL)",
                match,
                room);
        sql.update(
                "INSERT INTO"
                    + " game_match_players(id,match_id,room_member_id,user_id,status,joined_at)"
                    + " SELECT ?,?,id,?,'ACTIVE',now() FROM game_room_members WHERE room_id=?",
                player,
                match,
                user,
                room);
        var list =
                List.of("AIR", "BOOK", "ACTOR", "PLANET", "WATER").stream()
                        .map(a -> new WordlyWordProvider.Word(UUID.randomUUID(), a, "Meaning " + a))
                        .toList();
        words.initialize(match, list, List.of(player), Instant.now(), false);
        var pool = new ArrayList<>(list);
        pool.add(new WordlyWordProvider.Word(UUID.randomUUID(), "EAR", "Ear"));
        words.attachDictionary(match,words.prepareDictionary(pool));
    }

    @Test
    void dictionaryVersionsAreSharedAndKeepExistingMatchValidationStable() {
        String version=sql.queryForObject("SELECT wordly_dictionary_version FROM game_matches WHERE id=?",String.class,match);
        var catalog=sql.query("SELECT source_word_id,answer,meaning FROM game_wordly_dictionary_words WHERE version=? ORDER BY position",
            (rs,n)->new WordlyWordProvider.Word(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3)),version);
        int before=sql.queryForObject("SELECT count(*) FROM game_wordly_dictionary_words",Integer.class);
        assertEquals(version,words.prepareDictionary(catalog));
        assertEquals(before,sql.queryForObject("SELECT count(*) FROM game_wordly_dictionary_words",Integer.class));
        assertEquals(0,sql.queryForObject("SELECT count(*) FROM game_match_word_pool WHERE match_id=?",Integer.class,match));
        assertTrue(store.valid(match,"EAR"));
        var expanded=new ArrayList<>(catalog);
        expanded.add(new WordlyWordProvider.Word(UUID.randomUUID(),"ZEBRA","Zebra"));
        String newer=words.prepareDictionary(expanded);
        assertNotEquals(version,newer);
        assertFalse(store.valid(match,"ZEBRA"));
        assertTrue(store.valid(match,"EAR"));
    }

    @Test
    void concurrentCreatorsCommitOnlyOneCompleteDictionary() throws Exception {
        var catalog=List.of("ZEBRA","HORSE","SHEEP").stream()
            .map(a->new WordlyWordProvider.Word(UUID.randomUUID(),a,"Meaning "+a)).toList();
        var tx=new org.springframework.transaction.support.TransactionTemplate(new DataSourceTransactionManager(sql.getDataSource()));
        try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var gate=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<String> create=()->{gate.await();return tx.execute(t->words.prepareDictionary(catalog));};
            var first=executor.submit(create);var second=executor.submit(create);gate.countDown();
            String version=first.get(15,java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(version,second.get(15,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(3,sql.queryForObject("SELECT count(*) FROM game_wordly_dictionary_words WHERE version=?",Integer.class,version));
        }
    }

    @Test
    void legacyDictionaryPoolsStillAcceptGuessesAfterUpgrade() {
        sql.update("""
            INSERT INTO game_match_word_pool(match_id,position,source_word_id,answer,meaning)
            SELECT ?,position,source_word_id,answer,meaning FROM game_wordly_dictionary_words
            WHERE version=(SELECT wordly_dictionary_version FROM game_matches WHERE id=?)
            """,match,match);
        sql.update("UPDATE game_matches SET wordly_dictionary_version=NULL WHERE id=?",match);
        var current=words.current(match,user).orElseThrow();
        assertEquals(200,game.submit(match,current.playerWordId(),user,UUID.randomUUID(),"AIR").status());
    }

    @Test
    void completeUntimedMatchAndReplayWithoutDoubleScoring() {
        assertThrows(
                com.ieltspath.game.domain.exception.SoloGameplayException.class,
                () -> game.result(match, user));
        var first = words.current(match, user).orElseThrow();
        var request = UUID.randomUUID();
        var reply = game.submit(match, first.playerWordId(), user, request, "air");
        assertEquals(200, reply.status());
        assertEquals(reply, game.submit(match, first.playerWordId(), user, request, "AIR"));
        assertThrows(
                com.ieltspath.game.domain.exception.SoloGameplayException.class,
                () -> game.submit(match, first.playerWordId(), user, request, "EAR"));
        assertEquals(1, words.current(match, user).orElseThrow().currentStreak());
        assertEquals(1, words.current(match, user).orElseThrow().bestStreak());
        for (String answer : List.of("BOOK", "ACTOR", "PLANET", "WATER")) {
            var current = words.current(match, user).orElseThrow();
            assertEquals(
                    200,
                    game.submit(match, current.playerWordId(), user, UUID.randomUUID(), answer)
                            .status());
        }
        var result = game.result(match, user);
        var p = (Map<?, ?>) result.get("player");
        assertEquals(650L, ((Number) p.get("totalScore")).longValue());
        assertEquals(5, ((Number) p.get("wordsSolved")).intValue());
        assertEquals("WORD_LIMIT_REACHED", result.get("endReason"));
        assertEquals(
                "WAITING",
                sql.queryForObject("SELECT status FROM game_rooms WHERE id=?", String.class, room));
        assertEquals("WORD_LIMIT_REACHED", game.leave(match, user).body().get("endReason"));
    }

    @Test
    void invalidWordsDoNotConsumeAttemptsAndFailureConsumesOneLife() {
        var current = words.current(match, user).orElseThrow();
        assertEquals(
                400,
                game.submit(match, current.playerWordId(), user, UUID.randomUUID(), "ZZZ")
                        .status());
        assertEquals(0, words.current(match, user).orElseThrow().attemptsUsed());
        for (int i = 0; i < 4; i++)
            assertEquals(
                    200,
                    game.submit(match, current.playerWordId(), user, UUID.randomUUID(), "EAR")
                            .status());
        assertEquals(3, words.current(match, user).orElseThrow().livesRemaining());
        assertEquals(2, words.current(match, user).orElseThrow().wordNumber());
        var left = game.leave(match, user);
        assertEquals("PLAYER_LEFT", left.body().get("endReason"));
        assertEquals(left, game.leave(match, user));
        assertEquals(
                0L,
                ((Number) ((Map<?, ?>) game.result(match, user).get("player")).get("totalScore"))
                        .longValue());
    }

    @Test
    void timeoutIsAppliedOnceAndOnlyToTimedWords() {
        game.timeout(match, user);
        assertEquals(4, words.current(match, user).orElseThrow().livesRemaining());
        var current = words.current(match, user).orElseThrow();
        sql.update("UPDATE game_matches SET word_time_limit_seconds=180 WHERE id=?", match);
        sql.update(
                "UPDATE game_player_words SET started_at=now()-interval '181"
                    + " seconds',deadline_at=now()-interval '1 second' WHERE id=?",
                current.playerWordId());
        game.timeout(match, user);
        assertEquals(3, words.current(match, user).orElseThrow().livesRemaining());
        game.timeout(match, user);
        assertEquals(3, words.current(match, user).orElseThrow().livesRemaining());
    }

    @Test
    void concurrentRetryAndDifferentRequestsDoNotDoubleScore() throws Exception {
        var current = words.current(match, user).orElseThrow();
        UUID request = UUID.randomUUID();
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var a =
                    pool.submit(
                            () -> game.submit(match, current.playerWordId(), user, request, "AIR"));
            var b =
                    pool.submit(
                            () -> game.submit(match, current.playerWordId(), user, request, "AIR"));
            assertEquals(a.get().body(), b.get().body());
            assertEquals(
                    100L,
                    sql.queryForObject(
                            "SELECT score FROM game_match_players WHERE id=?", Long.class, player));
            var next = words.current(match, user).orElseThrow();
            var c =
                    pool.submit(
                            () ->
                                    game.submit(
                                            match,
                                            next.playerWordId(),
                                            user,
                                            UUID.randomUUID(),
                                            "BOOK"));
            var d =
                    pool.submit(
                            () ->
                                    game.submit(
                                            match,
                                            next.playerWordId(),
                                            user,
                                            UUID.randomUUID(),
                                            "BOOK"));
            assertEquals(Set.of(200, 409), Set.of(c.get().status(), d.get().status()));
            assertEquals(
                    200L,
                    sql.queryForObject(
                            "SELECT score FROM game_match_players WHERE id=?", Long.class, player));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void acceptedReceiptIsProcessedBeforeTimeoutEvenAfterDeadline() throws Exception {
        var current = words.current(match, user).orElseThrow();
        UUID request = UUID.randomUUID();
        sql.update("UPDATE game_matches SET word_time_limit_seconds=180 WHERE id=?", match);
        sql.update(
                "UPDATE game_player_words SET started_at=now()-interval '170"
                    + " seconds',deadline_at=now()+interval '10 seconds' WHERE id=?",
                current.playerWordId());
        var receipt = store.receive(match, current.playerWordId(), user, request, "AIR");
        sql.update(
                "UPDATE game_player_words SET started_at=?::timestamptz - interval '179"
                    + " seconds',deadline_at=?::timestamptz + interval '1 second' WHERE id=?",
                java.sql.Timestamp.from(receipt.received()),
                java.sql.Timestamp.from(receipt.received()),
                current.playerWordId());
        Thread.sleep(1100);
        game.timeout(match, user);
        assertEquals(200, store.receipt(request).reply().status());
        assertEquals(4, words.current(match, user).orElseThrow().livesRemaining());
    }

    @Test
    void exhaustingAllAttemptsEliminatesPlayerAndFinishesMatch() {
        int position = 7;
        for (String answer : List.of("BIRD", "OTHER", "ORANGE"))
            sql.update(
                    "INSERT INTO"
                        + " game_match_word_pool(match_id,position,source_word_id,answer,meaning)"
                        + " VALUES (?,?,gen_random_uuid(),?,'Fixture meaning')",
                    match,
                    position++,
                    answer);
        for (String wrong : List.of("EAR", "BIRD", "OTHER", "ORANGE")) {
            var current = words.current(match, user).orElseThrow();
            for (int attempt = 0; attempt < current.maxAttempts(); attempt++)
                assertEquals(
                        200,
                        game.submit(match, current.playerWordId(), user, UUID.randomUUID(), wrong)
                                .status());
        }
        var result = game.result(match, user);
        assertEquals("NO_LIVES_LEFT", result.get("endReason"));
        var p = (Map<?, ?>) result.get("player");
        assertEquals("ELIMINATED", p.get("status"));
        assertEquals(0, ((Number) p.get("livesRemaining")).intValue());
        assertEquals(4L, ((Number) p.get("wordsFailed")).longValue());
    }

    @Test
    void endlessAdvancesAndStartsANewDictionaryCycle() {
        sql.update("UPDATE game_matches SET play_mode='ENDLESS',word_limit=NULL WHERE id=?", match);
        String cycleFirst=sql.queryForObject("SELECT answer FROM game_wordly_dictionary_words WHERE version=(SELECT wordly_dictionary_version FROM game_matches WHERE id=?) AND position=1",String.class,match);
        for (String answer : List.of("AIR", "BOOK", "ACTOR", "PLANET", "WATER", "EAR", cycleFirst)) {
            var current = words.current(match, user).orElseThrow();
            assertEquals(
                    200,
                    game.submit(match, current.playerWordId(), user, UUID.randomUUID(), answer)
                            .status());
        }
        assertEquals(8, words.current(match, user).orElseThrow().wordNumber());
    }

    @Test
    void workerRecoversPendingReceiptAfterMatchFinished() {
        var current = words.current(match, user).orElseThrow();
        game.leave(match, user);
        UUID request = UUID.randomUUID();
        store.receive(match, current.playerWordId(), user, request, "AIR");
        assertTrue(store.due(50).contains(match));
        game.timeout(match, user);
        assertEquals(409, store.receipt(request).reply().status());
        assertEquals("MATCH_FINISHED", store.receipt(request).reply().body().get("code"));
        assertFalse(store.due(50).contains(match));
    }

    @Test
    void concurrentTimeoutSchedulesCostExactlyOneLife() throws Exception {
        var current = words.current(match, user).orElseThrow();
        int livesBefore = current.livesRemaining();
        sql.update("UPDATE game_matches SET word_time_limit_seconds=180 WHERE id=?", match);
        sql.update(
                "UPDATE game_player_words SET started_at=now()-interval '181 seconds',"
                        + "deadline_at=now()-interval '1 second' WHERE id=?",
                current.playerWordId());
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var barrier = new java.util.concurrent.CyclicBarrier(2);
        try {
            var runs =
                    java.util.stream.Stream.of(1, 2)
                            .map(
                                    i ->
                                            pool.submit(
                                                    () -> {
                                                        barrier.await();
                                                        game.timeout(match, user);
                                                        return null;
                                                    }))
                            .toList();
            for (var run : runs) run.get(30, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(livesBefore - 1, words.current(match, user).orElseThrow().livesRemaining());
    }
}
