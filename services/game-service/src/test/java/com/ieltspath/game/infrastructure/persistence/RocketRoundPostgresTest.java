package com.ieltspath.game.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.game.domain.aggregate.RocketRound;
import com.ieltspath.game.domain.vo.RocketDistractorPicker;
import com.ieltspath.game.domain.vo.RocketWord;
import com.ieltspath.game.infrastructure.persistence.adapter.RocketRoundRepositoryAdapter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class RocketRoundPostgresTest {
    @Container static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:15-alpine");

    JdbcTemplate sql;
    TransactionTemplate tx;
    RocketRoundRepositoryAdapter rounds;
    final Instant start = Instant.parse("2026-10-22T03:00:00Z");
    final List<RocketWord> bank = List.of(word("actor", "Diễn viên (nam)"), word("driver", "Tài xế"),
            word("career", "Sự nghiệp"), word("teacher", "Giáo viên"), word("doctor", "Bác sĩ"));

    static RocketWord word(String text, String meaning) {
        return new RocketWord(UUID.nameUUIDFromBytes(text.getBytes()), text, meaning);
    }

    @BeforeEach void setup() {
        var ds = new DriverManagerDataSource(db.getJdbcUrl(), db.getUsername(), db.getPassword());
        Flyway.configure().dataSource(ds).load().migrate();
        sql = new JdbcTemplate(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        rounds = new RocketRoundRepositoryAdapter(sql, new ObjectMapper().findAndRegisterModules());
    }

    UUID match(String playerMode, int questions) {
        UUID match = UUID.randomUUID();
        sql.update("""
            INSERT INTO game_matches(id,game_type,learning_domain,config_snapshot,content_snapshot,status,started_at,created_at,
                player_mode,question_count,seconds_per_question,initial_lives)
            VALUES (?,'ROCKET','VOCABULARY','{}','{}','IN_PROGRESS',now(),now(),?,?,15,4)
            """, match, playerMode, questions);
        return match;
    }

    UUID player(UUID match) {
        UUID player = UUID.randomUUID();
        sql.update("INSERT INTO game_match_players(id,match_id,user_id,status,joined_at) VALUES (?,?,?,'ACTIVE',now())",
                player, match, UUID.randomUUID());
        return player;
    }

    RocketRound round(UUID match, int sequence) {
        var answer = bank.get(sequence - 1);
        var pick = RocketDistractorPicker.pick(answer, bank, RocketDistractorPicker.seed(match, sequence));
        return new RocketRound(UUID.randomUUID(), match, sequence, answer.sourceWordId(), answer.meaning(), pick.options(),
                pick.correctOptionId());
    }

    @Test void roundsAndAnswersSurviveARoundTrip() {
        UUID match = match("MULTIPLAYER", 5);
        UUID alice = player(match);
        UUID bob = player(match);
        var first = round(match, 1);
        var second = round(match, 2);
        rounds.createAll(List.of(second, first));

        var loaded = rounds.findByMatch(match);
        assertEquals(List.of(1, 2), loaded.stream().map(RocketRound::sequence).toList());
        assertEquals(first.options(), loaded.getFirst().options());
        assertEquals(first.meaning(), loaded.getFirst().meaning());
        assertTrue(loaded.getFirst().revealedCorrectOptionId().isEmpty());

        first.open(start, Duration.ofSeconds(15));
        first.accept(alice, first.correctOptionId(), "req-a", start.plusSeconds(3));
        rounds.save(first);
        tx.executeWithoutResult(t -> {
            var locked = rounds.findForUpdate(first.id()).orElseThrow();
            assertEquals(RocketRound.Status.OPEN, locked.status());
            assertEquals(start.plusSeconds(15), locked.deadlineAt());
            var stored = locked.answerOf(alice).orElseThrow();
            assertTrue(stored.correct());
            assertEquals(180, stored.points());
            assertEquals("req-a", stored.requestId());
            // A retry after reload still replays rather than answering twice.
            assertSame(stored, locked.accept(alice, first.correctOptionId(), "req-a", start.plusSeconds(9)));
            locked.close(start.plusSeconds(15), RocketRound.CloseReason.DEADLINE, Set.of(alice, bob));
            rounds.save(locked);
            rounds.save(locked);
        });

        var closed = rounds.findByMatch(match).getFirst();
        assertEquals(RocketRound.Status.CLOSED, closed.status());
        assertEquals(RocketRound.CloseReason.DEADLINE, closed.closeReason());
        assertEquals(first.correctOptionId(), closed.revealedCorrectOptionId().orElseThrow());
        assertFalse(closed.answerOf(bob).orElseThrow().answered());
        assertEquals(2, sql.queryForObject("SELECT count(*) FROM game_rocket_answers WHERE round_id=?", Integer.class, first.id()));
        assertEquals(RocketRound.Status.PENDING, rounds.findByMatch(match).get(1).status());
    }

    @Test void dueMatchesListsExpiredAndScheduledRoundsOfRunningMatches() {
        var store = new com.ieltspath.game.infrastructure.persistence.adapter.RocketMatchStoreAdapter(sql);
        UUID match = match("SOLO", 3);
        var first = round(match, 1);
        var second = round(match, 2);
        rounds.createAll(List.of(first, second));
        first.open(start, Duration.ofSeconds(15));
        rounds.save(first);

        assertEquals(List.of(), store.dueMatches(start.plusSeconds(14), 10));
        assertEquals(List.of(match), store.dueMatches(start.plusSeconds(15), 10));

        tx.executeWithoutResult(t -> {
            var locked = rounds.findForUpdate(first.id()).orElseThrow();
            locked.close(start.plusSeconds(15), RocketRound.CloseReason.DEADLINE, Set.of());
            rounds.save(locked);
        });
        second.schedule(start.plusSeconds(18));
        rounds.save(second);
        assertEquals(List.of(), store.dueMatches(start.plusSeconds(17), 10));
        assertEquals(List.of(match), store.dueMatches(start.plusSeconds(18), 10));
        assertEquals(start.plusSeconds(18), rounds.findByMatch(match).get(1).opensAt());

        sql.update("UPDATE game_matches SET status='COMPLETED', end_reason='PLAYER_LEFT', ended_at=now() WHERE id=?", match);
        assertEquals(List.of(), store.dueMatches(start.plusSeconds(60), 10));
    }

    @Test void schemaGuardsRoundAndAnswerIntegrity() {
        UUID match = match("MULTIPLAYER", 5);
        UUID alice = player(match);
        var first = round(match, 1);
        var second = round(match, 2);
        rounds.createAll(List.of(first, second));
        first.open(start, Duration.ofSeconds(15));
        rounds.save(first);
        second.open(start, Duration.ofSeconds(15));
        assertThrows(DataIntegrityViolationException.class, () -> rounds.save(second), "only one open round per match");

        UUID otherMatch = match("SOLO", 3);
        UUID stranger = player(otherMatch);
        first.accept(stranger, first.correctOptionId(), "req-x", start.plusSeconds(1));
        assertThrows(DataIntegrityViolationException.class, () -> rounds.save(first), "players answer only their own match");

        var fresh = rounds.findByMatch(match).getFirst();
        fresh.accept(alice, fresh.correctOptionId(), "req-1", start.plusSeconds(1));
        rounds.save(fresh);
        assertThrows(DataIntegrityViolationException.class, () -> sql.update("""
            INSERT INTO game_rocket_answers(id,round_id,match_id,player_id,option_id,correct,points,received_at,response_ms,request_id)
            VALUES (gen_random_uuid(),?,?,?,?,true,150,now(),10,'req-1')
            """, second.id(), match, alice, second.correctOptionId()), "request ids are unique per player");
        assertThrows(DataIntegrityViolationException.class, () -> sql.update("""
            INSERT INTO game_rocket_answers(id,round_id,match_id,player_id,option_id,correct,points,received_at,response_ms,request_id)
            VALUES (gen_random_uuid(),?,?,?,?,false,150,now(),10,'req-2')
            """, second.id(), match, alice, second.correctOptionId()), "wrong answers score nothing");
    }

    @Test void rocketRoomsAndMatchesNeedRocketSettings() {
        String insertRoom = """
            INSERT INTO game_rooms(id,room_code,host_user_id,game_type,learning_domain,mode,max_players,config_snapshot,status,
                created_at,updated_at,player_mode,initial_lives,question_count,seconds_per_question)
            VALUES (gen_random_uuid(),?,gen_random_uuid(),'ROCKET','VOCABULARY','PRACTICE',?,'{}','WAITING',now(),now(),?,4,?,?)
            """;
        assertEquals(1, sql.update(insertRoom, "SOLO03", 1, "SOLO", 3, 15));
        assertEquals(1, sql.update(insertRoom, "MULT10", 4, "MULTIPLAYER", 10, 20));
        assertThrows(DataIntegrityViolationException.class, () -> sql.update(insertRoom, "MULT03", 4, "MULTIPLAYER", 3, 15));
        assertThrows(DataIntegrityViolationException.class, () -> sql.update(insertRoom, "SOLO12", 1, "SOLO", 6, 12));
        assertThrows(DataIntegrityViolationException.class, () -> sql.update(insertRoom, "SOLO04", 4, "SOLO", 6, 15));
        assertThrows(DataIntegrityViolationException.class, () -> sql.update("""
            INSERT INTO game_rooms(id,room_code,host_user_id,game_type,learning_domain,mode,max_players,config_snapshot,status,
                created_at,updated_at,player_mode)
            VALUES (gen_random_uuid(),'NOSETS',gen_random_uuid(),'ROCKET','VOCABULARY','PRACTICE',1,'{}','WAITING',now(),now(),'SOLO')
            """), "a Rocket room needs its settings");
        assertThrows(DataIntegrityViolationException.class, () -> sql.update("""
            INSERT INTO game_matches(id,game_type,learning_domain,config_snapshot,content_snapshot,status,started_at,created_at,player_mode)
            VALUES (gen_random_uuid(),'ROCKET','VOCABULARY','{}','{}','IN_PROGRESS',now(),now(),'SOLO')
            """), "a Rocket match needs its settings");
    }
}
