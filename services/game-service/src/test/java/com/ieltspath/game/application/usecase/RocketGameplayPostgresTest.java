package com.ieltspath.game.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.game.application.result.RocketGameplayResults.Round;
import com.ieltspath.game.application.result.RocketRoundEvents;
import com.ieltspath.game.domain.aggregate.RocketRound;
import com.ieltspath.game.domain.exception.GameMatchNotFoundException;
import com.ieltspath.game.domain.exception.SoloGameplayException;
import com.ieltspath.game.domain.vo.RocketEndReason;
import com.ieltspath.game.domain.vo.RocketRoomSettings;
import com.ieltspath.game.domain.vo.RocketWord;
import com.ieltspath.game.infrastructure.persistence.adapter.RocketMatchStoreAdapter;
import com.ieltspath.game.infrastructure.persistence.adapter.RocketRoundRepositoryAdapter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class RocketGameplayPostgresTest {
    @Container static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:15-alpine");

    JdbcTemplate sql;
    RocketRoundRepositoryAdapter rounds;
    RocketMatchStoreAdapter store;
    RocketGameplayUseCase gameplay;
    final List<Object> published = new java.util.concurrent.CopyOnWriteArrayList<>();
    Instant now = Instant.parse("2026-10-22T05:00:00Z");
    final UUID user = UUID.randomUUID();
    final List<RocketWord> bank = List.of(word("actor", "Diễn viên (nam)"), word("driver", "Tài xế"),
            word("career", "Sự nghiệp"), word("teacher", "Giáo viên"), word("doctor", "Bác sĩ"),
            word("journey", "Hành trình"), word("library", "Thư viện"), word("weather", "Thời tiết"));

    static RocketWord word(String text, String meaning) {
        return new RocketWord(UUID.nameUUIDFromBytes(text.getBytes()), text, meaning);
    }

    @BeforeEach void setup() {
        var ds = new DriverManagerDataSource(db.getJdbcUrl(), db.getUsername(), db.getPassword());
        Flyway.configure().dataSource(ds).load().migrate();
        sql = new JdbcTemplate(ds);
        rounds = new RocketRoundRepositoryAdapter(sql, new ObjectMapper().findAndRegisterModules());
        store = new RocketMatchStoreAdapter(sql) {
            @Override public Instant now() { return now; }
        };
        gameplay = new RocketGameplayUseCase(store, rounds, new DataSourceTransactionManager(ds), published::add);
    }

    /** A started solo match as StartRocketMatchUseCase leaves it: room IN_MATCH, one player, round 1 open. */
    UUID start(int questions) {
        UUID room = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        UUID match = UUID.randomUUID();
        sql.update("""
            INSERT INTO game_rooms(id,room_code,host_user_id,game_type,learning_domain,mode,max_players,config_snapshot,status,
                created_at,updated_at,player_mode,initial_lives,question_count,seconds_per_question)
            VALUES (?,upper(substring(md5(?::text),1,6)),?,'ROCKET','VOCABULARY','PRACTICE',1,'{}','IN_MATCH',now(),now(),'SOLO',4,?,15)
            """, room, room, user, questions);
        sql.update("""
            INSERT INTO game_room_members(id,room_id,user_id,member_role,status,joined_at,slot_number,display_name)
            VALUES (?,?,?,'HOST','READY',now(),1,'Player')
            """, member, room, user);
        sql.update("""
            INSERT INTO game_matches(id,room_id,game_type,learning_domain,config_snapshot,content_snapshot,status,started_at,
                created_at,player_mode,initial_lives,question_count,seconds_per_question)
            VALUES (?,?,'ROCKET','VOCABULARY','{}','{}','IN_PROGRESS',?,?,'SOLO',4,?,15)
            """, match, room, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now), questions);
        sql.update("""
            INSERT INTO game_match_players(id,match_id,room_member_id,user_id,status,joined_at,lives_remaining)
            VALUES (gen_random_uuid(),?,?,?,'ACTIVE',now(),4)
            """, match, member, user);
        var prepared = StartRocketMatchUseCase.prepareRounds(match, RocketRoomSettings.solo(questions), bank);
        rounds.createAll(prepared);
        prepared.getFirst().open(now, Duration.ofSeconds(15));
        rounds.save(prepared.getFirst());
        return match;
    }

    UUID correct(UUID match, Round round) {
        String answer = bank.stream().filter(w -> w.meaning().equals(round.meaning())).findFirst().orElseThrow().word();
        return round.options().stream().filter(o -> o.word().equals(answer)).findFirst().orElseThrow().optionId();
    }

    UUID wrong(UUID match, Round round) {
        UUID right = correct(match, round);
        return round.options().stream().map(o -> o.optionId()).filter(id -> !id.equals(right)).findFirst().orElseThrow();
    }

    static void assertCode(String code, Executable action) {
        assertEquals(code, assertThrows(SoloGameplayException.class, action).code());
    }

    @Test void aCorrectAnswerClosesTheRoundAndSchedulesTheNextAfterTheReveal() {
        UUID match = start(3);
        var current = gameplay.current(match, user);
        assertEquals(new com.ieltspath.game.application.result.RocketGameplayResults.Settings(3, 4, 15), current.settings());
        var round = current.round();
        assertEquals(1, round.number());
        assertEquals(now.plusSeconds(15), round.deadlineAt());
        assertNull(current.lastRoundResult());

        now = now.plusSeconds(3);
        var ack = gameplay.answer(match, round.roundId(), user, "r1", correct(match, round));
        var result = ack.roundResult();
        assertTrue(result.correct());
        assertEquals(180, result.points());
        assertEquals(180, result.totalScore());
        assertEquals(4, result.livesRemaining());
        assertEquals(now.plusMillis(2500), result.nextRoundAt());
        assertFalse(result.matchFinished());

        var waiting = gameplay.current(match, user);
        assertEquals(2, waiting.round().number());
        assertEquals(now.plusMillis(2500), waiting.round().opensAt());
        assertNull(waiting.round().deadlineAt());
        assertEquals(round.roundId(), waiting.lastRoundResult().roundId());

        now = now.plusMillis(2500);
        assertTrue(store.dueMatches(now, 100).contains(match));
        var opened = gameplay.current(match, user).round();
        assertEquals(now, opened.opensAt());
        assertEquals(now.plusSeconds(15), opened.deadlineAt());
        assertFalse(store.dueMatches(now, 100).contains(match));
    }

    @Test void retriesReplayAndConflictsAreRejected() {
        UUID match = start(3);
        var round = gameplay.current(match, user).round();
        now = now.plusSeconds(1);
        var first = gameplay.answer(match, round.roundId(), user, "r1", wrong(match, round));
        now = now.plusSeconds(1);
        var again = gameplay.answer(match, round.roundId(), user, "r1", wrong(match, round));
        assertEquals(first.roundResult(), again.roundResult());
        assertEquals(3, again.roundResult().livesRemaining());
        assertCode("REQUEST_ID_CONFLICT", () -> gameplay.answer(match, round.roundId(), user, "r1", correct(match, round)));
        assertCode("ROUND_CLOSED", () -> gameplay.answer(match, round.roundId(), user, "r2", correct(match, round)));
        assertEquals(1, sql.queryForObject("SELECT count(*) FROM game_rocket_answers WHERE round_id=?", Integer.class,
                round.roundId()));
    }

    @Test void aRetryAfterTheNextRoundOpenedGetsTheSameResult() {
        UUID match = start(3);
        var round = gameplay.current(match, user).round();
        now = now.plusSeconds(2);
        var first = gameplay.answer(match, round.roundId(), user, "r1", correct(match, round));
        now = now.plusSeconds(4);
        var next = gameplay.current(match, user).round();
        assertEquals(now, next.opensAt(), "opened later than scheduled");
        var retry = gameplay.answer(match, round.roundId(), user, "r1", correct(match, round));
        assertEquals(first.roundResult(), retry.roundResult());
        assertEquals(first.roundResult(), gameplay.current(match, user).lastRoundResult());
    }

    @Test void reusingARequestIdOnAnotherRoundIsAConflict() {
        UUID match = start(3);
        var round = gameplay.current(match, user).round();
        now = now.plusSeconds(1);
        gameplay.answer(match, round.roundId(), user, "r1", wrong(match, round));
        now = now.plusSeconds(3);
        var next = gameplay.current(match, user).round();
        assertCode("REQUEST_ID_CONFLICT", () -> gameplay.answer(match, next.roundId(), user, "r1", wrong(match, next)));
        assertFalse(gameplay.current(match, user).round().me().answered());
        assertNotNull(gameplay.answer(match, next.roundId(), user, "r2", wrong(match, next)).roundResult());
    }

    @Test void theDeadlineCostsALifeEvenIfTheLateAnswerIsRejected() {
        UUID match = start(3);
        var round = gameplay.current(match, user).round();
        now = now.plusSeconds(16);
        assertCode("ROUND_CLOSED", () -> gameplay.answer(match, round.roundId(), user, "late", correct(match, round)));
        var current = gameplay.current(match, user);
        var miss = current.lastRoundResult();
        assertEquals(round.roundId(), miss.roundId());
        assertNull(miss.chosenOptionId());
        assertEquals(3, miss.livesRemaining());
        assertEquals(Instant.parse("2026-10-22T05:00:15Z").plusMillis(2500), miss.nextRoundAt());
        // The next round waits out the reveal pause counted from the deadline, then opens when first seen after it.
        assertEquals(miss.nextRoundAt(), current.round().opensAt());
        assertNull(current.round().deadlineAt());
        now = now.plusSeconds(2);
        assertEquals(now, gameplay.current(match, user).round().opensAt());
        assertEquals("CLOSED", sql.queryForObject("SELECT status FROM game_rocket_rounds WHERE id=?", String.class, round.roundId()));
    }

    @Test void finishingAllRoundsCompletesTheMatchAndFreesTheRoom() {
        UUID match = start(3);
        for (int i = 0; i < 3; i++) {
            var round = gameplay.current(match, user).round();
            assertEquals(i + 1, round.number());
            now = now.plusSeconds(2);
            var ack = gameplay.answer(match, round.roundId(), user, "c" + i, correct(match, round));
            assertEquals(i == 2, ack.roundResult().matchFinished());
            now = now.plusSeconds(3);
        }
        var current = gameplay.current(match, user);
        assertTrue(current.matchFinished());
        assertNull(current.round());
        assertEquals(RocketEndReason.ALL_ROUNDS_COMPLETED, current.lastRoundResult().endReason());
        assertEquals("FINISHED", current.lastRoundResult().status());
        assertEquals(3 * 186, current.lastRoundResult().totalScore());
        assertEquals("WAITING", sql.queryForObject("SELECT r.status FROM game_rooms r JOIN game_matches m ON m.room_id=r.id WHERE m.id=?",
                String.class, match));
        assertEquals(3, sql.queryForObject("SELECT words_solved FROM game_match_players WHERE match_id=?", Integer.class, match));
        assertCode("MATCH_FINISHED", () -> gameplay.answer(match, current.lastRoundResult().roundId(), user, "after",
                current.lastRoundResult().correctOptionId()));
        assertFalse(store.dueMatches(now.plusSeconds(60), 100).contains(match));
    }

    @Test void runningOutOfLivesEndsTheMatchEarly() {
        UUID match = start(6);
        for (int i = 0; i < 4; i++) {
            var round = gameplay.current(match, user).round();
            now = now.plusSeconds(1);
            gameplay.answer(match, round.roundId(), user, "w" + i, wrong(match, round));
            now = now.plusSeconds(3);
        }
        var current = gameplay.current(match, user);
        assertTrue(current.matchFinished());
        assertEquals(RocketEndReason.NO_LIVES_LEFT, current.lastRoundResult().endReason());
        assertEquals("ELIMINATED", current.lastRoundResult().status());
        assertEquals(0, current.lastRoundResult().livesRemaining());
        assertEquals(2L, (long) sql.queryForObject(
                "SELECT count(*) FROM game_rocket_rounds WHERE match_id=? AND status='PENDING'", Long.class, match));
    }

    @Test void leavingForfeitsKeepsPointsAndClosesTheSoloRoom() {
        UUID match = start(6);
        var first = gameplay.current(match, user).round();
        now = now.plusSeconds(5);
        gameplay.answer(match, first.roundId(), user, "c1", correct(match, first));
        now = now.plusSeconds(3);
        var open = gameplay.current(match, user).round();
        assertCode("MATCH_NOT_FINISHED", () -> gameplay.result(match, user));

        now = now.plusSeconds(1);
        var left = gameplay.leave(match, user);
        assertEquals("COMPLETED", left.status());
        assertEquals(RocketEndReason.PLAYER_LEFT, left.endReason());
        assertEquals("FORFEITED", left.playerStatus());
        assertEquals(166, left.totalScore());
        assertTrue(left.resultAvailable());
        assertEquals(left, gameplay.leave(match, user), "leaving again changes nothing");

        assertEquals("MATCH_ENDED", sql.queryForObject("SELECT close_reason FROM game_rocket_rounds WHERE id=?", String.class,
                open.roundId()));
        assertEquals(0, sql.queryForObject("SELECT count(*) FROM game_rocket_answers WHERE round_id=?", Integer.class,
                open.roundId()));
        assertEquals(true, sql.queryForObject("SELECT r.is_delete FROM game_rooms r JOIN game_matches m ON m.room_id=r.id WHERE m.id=?",
                Boolean.class, match));
        assertFalse(store.dueMatches(now.plusSeconds(60), 100).contains(match));

        var result = gameplay.result(match, user);
        assertEquals(RocketEndReason.PLAYER_LEFT, result.endReason());
        assertEquals(6, result.totalRounds());
        assertEquals(1, result.answered());
        assertEquals(1, result.correctCount());
        assertEquals(4, result.livesRemaining());
        assertEquals(166, result.totalScore());
        assertEquals("FORFEITED", result.playerStatus());
        assertEquals(9000, result.durationMs());
        assertCode("MATCH_FINISHED", () -> gameplay.answer(match, open.roundId(), user, "late", correct(match, open)));
    }

    @Test void resultOfACompletedMatchAndLeavingAfterwardsKeepsTheReason() {
        UUID match = start(3);
        for (int i = 0; i < 3; i++) {
            var round = gameplay.current(match, user).round();
            now = now.plusSeconds(1);
            gameplay.answer(match, round.roundId(), user, "x" + i, i == 1 ? wrong(match, round) : correct(match, round));
            now = now.plusSeconds(3);
        }
        var left = gameplay.leave(match, user);
        assertEquals(RocketEndReason.ALL_ROUNDS_COMPLETED, left.endReason());
        assertEquals("FINISHED", left.playerStatus());
        var result = gameplay.result(match, user);
        assertEquals(3, result.answered());
        assertEquals(2, result.correctCount());
        assertEquals(3, result.livesRemaining());
        assertEquals(2 * 193, result.totalScore());
        assertEquals(false, sql.queryForObject("SELECT r.is_delete FROM game_rooms r JOIN game_matches m ON m.room_id=r.id WHERE m.id=?",
                Boolean.class, match));
        assertThrows(GameMatchNotFoundException.class, () -> gameplay.result(match, UUID.randomUUID()));
        assertTrue(gameplay.isRocket(match));
        assertFalse(gameplay.isRocket(UUID.randomUUID()));
    }

    @Test void finishingRanksByScoreThenCorrectTimeThenJoinOrder() {
        UUID match = start(3);
        UUID room = sql.queryForObject("SELECT room_id FROM game_matches WHERE id=?", UUID.class, match);
        // user (joined first): 300 in 5 s; b: 300 in 4 s; c: 300 in 5 s, joined later; d: 100.
        sql.update("UPDATE game_match_players SET score=300, total_solve_time_ms=5000, joined_at=? WHERE match_id=?",
                java.sql.Timestamp.from(now), match);
        for (String[] p : new String[][]{{"b", "300", "4000", "2"}, {"c", "300", "5000", "3"}, {"d", "100", "1000", "4"}}) {
            UUID member = UUID.randomUUID();
            sql.update("""
                INSERT INTO game_room_members(id,room_id,user_id,member_role,status,joined_at,slot_number,display_name)
                VALUES (?,?,?,'PLAYER','READY',now(),?,?)
                """, member, room, UUID.randomUUID(), Integer.parseInt(p[3]), p[0]);
            sql.update("""
                INSERT INTO game_match_players(id,match_id,room_member_id,user_id,status,joined_at,lives_remaining,score,total_solve_time_ms)
                SELECT gen_random_uuid(),?,id,user_id,'ACTIVE',?,4,?,? FROM game_room_members WHERE id=?
                """, match, java.sql.Timestamp.from(now.plusSeconds(Integer.parseInt(p[3]))), Long.parseLong(p[1]),
                    Long.parseLong(p[2]), member);
        }
        store.finish(match, room, RocketEndReason.ALL_ROUNDS_COMPLETED, now);
        assertEquals(List.of("b", "Player", "c", "d"), sql.queryForList("""
            SELECT rm.display_name FROM game_match_players p JOIN game_room_members rm ON rm.id=p.room_member_id
            WHERE p.match_id=? ORDER BY p.rank
            """, String.class, match));
    }

    /** Adds a second ACTIVE player to a started match and returns their user id. */
    UUID addPlayer(UUID match) {
        UUID room = sql.queryForObject("SELECT room_id FROM game_matches WHERE id=?", UUID.class, match);
        UUID member = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        sql.update("""
            INSERT INTO game_room_members(id,room_id,user_id,member_role,status,joined_at,slot_number,display_name)
            VALUES (?,?,?,'PLAYER','READY',now(),
                (SELECT COALESCE(max(slot_number),0)+1 FROM game_room_members WHERE room_id=?),'Guest')
            """, member, room, other, room);
        sql.update("""
            INSERT INTO game_match_players(id,match_id,room_member_id,user_id,status,joined_at,lives_remaining)
            VALUES (gen_random_uuid(),?,?,?,'ACTIVE',now(),4)
            """, match, member, other);
        return other;
    }

    @Test void roundChangesAreAnnouncedToTheMatch() {
        List<Object> published = new java.util.ArrayList<>();
        gameplay = new RocketGameplayUseCase(store, rounds, new DataSourceTransactionManager(
                new DriverManagerDataSource(db.getJdbcUrl(), db.getUsername(), db.getPassword())), published::add);
        UUID match = start(3);
        UUID guest = addPlayer(match);
        var round = gameplay.current(match, user).round();

        now = now.plusSeconds(2);
        gameplay.answer(match, round.roundId(), user, "h1", correct(match, round));
        var answered = assertInstanceOf(com.ieltspath.game.application.result.RocketRoundEvents.Answered.class, published.getLast());
        assertEquals(user, answered.userId());
        assertEquals(1, answered.answeredCount());
        assertEquals(2, answered.aliveCount());

        now = now.plusSeconds(1);
        gameplay.answer(match, round.roundId(), guest, "g1", wrong(match, round));
        var closed = assertInstanceOf(com.ieltspath.game.application.result.RocketRoundEvents.Closed.class, published.getLast());
        assertEquals(round.roundId(), closed.roundId());
        assertEquals(correct(match, round), closed.correctOptionId());
        assertEquals(now.plusMillis(2500), closed.nextRoundAt(), "start() makes a solo-mode match");
        assertFalse(closed.matchFinished());
        var byUser = closed.answers().stream().collect(java.util.stream.Collectors.toMap(
                com.ieltspath.game.application.result.RocketRoundEvents.PlayerOutcome::userId, o -> o));
        assertTrue(byUser.get(user).correct());
        assertEquals(4, byUser.get(user).livesRemaining());
        assertFalse(byUser.get(guest).correct());
        assertEquals(3, byUser.get(guest).livesRemaining());

        int before = published.size();
        gameplay.current(match, user);
        assertEquals(before, published.size(), "nothing opens during the reveal pause");
        now = now.plusSeconds(3);
        gameplay.advance(match);
        var started = assertInstanceOf(com.ieltspath.game.application.result.RocketRoundEvents.Started.class, published.getLast());
        assertEquals(2, started.number());
        assertEquals(now.plusSeconds(15), started.deadlineAt());
        assertTrue(java.util.Arrays.stream(started.getClass().getRecordComponents())
                .noneMatch(c -> c.getName().toLowerCase().contains("correct")), "a started round never names its answer");
        gameplay.advance(match);
        assertEquals(before + 1, published.size(), "settling again announces nothing new");

        now = now.plusSeconds(16);
        gameplay.advance(match);
        var missed = assertInstanceOf(com.ieltspath.game.application.result.RocketRoundEvents.Closed.class, published.getLast());
        assertTrue(missed.answers().stream().allMatch(o -> o.optionId() == null && !o.correct()));
    }

    @Test void theLastRoundAnnouncesTheEnd() {
        List<Object> published = new java.util.ArrayList<>();
        gameplay = new RocketGameplayUseCase(store, rounds, new DataSourceTransactionManager(
                new DriverManagerDataSource(db.getJdbcUrl(), db.getUsername(), db.getPassword())), published::add);
        UUID match = start(3);
        for (int i = 0; i < 3; i++) {
            var round = gameplay.current(match, user).round();
            now = now.plusSeconds(1);
            gameplay.answer(match, round.roundId(), user, "e" + i, correct(match, round));
            now = now.plusSeconds(3);
        }
        var last = assertInstanceOf(com.ieltspath.game.application.result.RocketRoundEvents.Closed.class, published.getLast());
        assertTrue(last.matchFinished());
        assertEquals(RocketEndReason.ALL_ROUNDS_COMPLETED, last.endReason());
        assertNull(last.nextRoundAt());
        assertEquals("FINISHED", last.answers().getFirst().status());
    }

    /** A started multiplayer match: the host ({@link #user}) plus the given number of guests, round 1 open. */
    UUID startMultiplayer(List<UUID> guests) {
        UUID match = start(5);
        sql.update("UPDATE game_matches SET player_mode='MULTIPLAYER' WHERE id=?", match);
        sql.update("UPDATE game_rooms SET player_mode='MULTIPLAYER', max_players=4 WHERE id=(SELECT room_id FROM game_matches WHERE id=?)", match);
        for (int i = 0; i < guests.size(); i++) guests.set(i, addPlayer(match));
        return match;
    }

    List<Object> recordEvents() {
        List<Object> published = new java.util.ArrayList<>();
        gameplay = new RocketGameplayUseCase(store, rounds, new DataSourceTransactionManager(
                new DriverManagerDataSource(db.getJdbcUrl(), db.getUsername(), db.getPassword())), published::add);
        return published;
    }

    String memberStatus(UUID match, UUID userId) {
        return sql.queryForObject("""
            SELECT rm.status FROM game_room_members rm JOIN game_matches m ON m.room_id=rm.room_id WHERE m.id=? AND rm.user_id=?
            """, String.class, match, userId);
    }

    @Test void leavingAMultiplayerMatchLetsTheOthersPlayOn() {
        var published = recordEvents();
        List<UUID> guests = new java.util.ArrayList<>(java.util.Arrays.asList(null, null));
        UUID match = startMultiplayer(guests);
        var round = gameplay.current(match, user).round();
        now = now.plusSeconds(2);
        gameplay.answer(match, round.roundId(), user, "h1", correct(match, round));

        var left = gameplay.leave(match, guests.get(0));
        assertEquals("FORFEITED", left.playerStatus());
        assertFalse(left.resultAvailable());
        assertEquals("LEFT", memberStatus(match, guests.get(0)));
        assertTrue(published.stream().anyMatch(e -> e instanceof com.ieltspath.game.application.result.WordlyPlayerLeftEvent));
        assertEquals("OPEN", sql.queryForObject("SELECT status FROM game_rocket_rounds WHERE id=?", String.class, round.roundId()),
                "the round still waits for the other guest");

        now = now.plusSeconds(1);
        var ack = gameplay.answer(match, round.roundId(), guests.get(1), "g1", wrong(match, round));
        assertNotNull(ack.roundResult(), "the last player still in the round closes it");
        assertFalse(ack.roundResult().matchFinished());
        assertEquals("IN_PROGRESS", sql.queryForObject("SELECT status FROM game_matches WHERE id=?", String.class, match));
    }

    @Test void whenTheLastOpponentLeavesTheAnswerAlreadyGivenStillCounts() {
        var published = recordEvents();
        List<UUID> guests = new java.util.ArrayList<>(java.util.Arrays.asList((UUID) null));
        UUID match = startMultiplayer(guests);
        var round = gameplay.current(match, user).round();
        now = now.plusSeconds(2);
        gameplay.answer(match, round.roundId(), user, "h1", correct(match, round));
        gameplay.leave(match, guests.get(0));

        var current = gameplay.current(match, user);
        assertTrue(current.matchFinished());
        assertEquals(RocketEndReason.LAST_PLAYER_STANDING, current.lastRoundResult().endReason());
        assertTrue(current.lastRoundResult().correct());
        assertTrue(current.lastRoundResult().totalScore() > 0);
        var closed = assertInstanceOf(com.ieltspath.game.application.result.RocketRoundEvents.Closed.class,
                published.stream().filter(e -> e instanceof com.ieltspath.game.application.result.RocketRoundEvents.Closed).reduce((a, b) -> b).orElseThrow());
        assertTrue(closed.matchFinished());
        assertEquals("WAITING", sql.queryForObject("SELECT r.status FROM game_rooms r JOIN game_matches m ON m.room_id=r.id WHERE m.id=?",
                String.class, match), "the host stayed, so the room goes back to the lobby");
    }

    @Test void theHostLeavingEndsAndClosesTheRoom() {
        var published = recordEvents();
        List<UUID> guests = new java.util.ArrayList<>(java.util.Arrays.asList((UUID) null));
        UUID match = startMultiplayer(guests);
        gameplay.current(match, user);
        gameplay.leave(match, user);
        var current = gameplay.current(match, guests.get(0));
        assertTrue(current.matchFinished());
        assertEquals("CLOSED", sql.queryForObject("SELECT r.status FROM game_rooms r JOIN game_matches m ON m.room_id=r.id WHERE m.id=?",
                String.class, match));
        assertTrue(published.stream().anyMatch(e -> e instanceof com.ieltspath.game.application.result.RoomClosedEvent));
        assertEquals("MATCH_ENDED", sql.queryForObject(
                "SELECT close_reason FROM game_rocket_rounds WHERE match_id=? AND sequence=1", String.class, match),
                "nobody answered, so the open round just ends");
    }

    @Test void anEliminatedHostWhoLeavesClosesTheRoomWhenTheMatchEnds() {
        var published = recordEvents();
        List<UUID> guests = new java.util.ArrayList<>(java.util.Arrays.asList(null, null));
        UUID match = startMultiplayer(guests);
        gameplay.current(match, user);
        sql.update("UPDATE game_match_players SET status='ELIMINATED', lives_remaining=0 WHERE match_id=? AND user_id=?", match, user);

        var left = gameplay.leave(match, user);
        assertEquals("ELIMINATED", left.playerStatus(), "already out, so not counted as a forfeit");
        assertEquals("LEFT", memberStatus(match, user));
        assertEquals("IN_PROGRESS", sql.queryForObject("SELECT status FROM game_matches WHERE id=?", String.class, match),
                "the guests play on");

        gameplay.leave(match, guests.get(0));
        assertTrue(gameplay.current(match, guests.get(1)).matchFinished());
        assertEquals("CLOSED", sql.queryForObject("SELECT r.status FROM game_rooms r JOIN game_matches m ON m.room_id=r.id WHERE m.id=?",
                String.class, match), "the host is gone, so the room closes instead of going back to the lobby");
        assertTrue(published.stream().anyMatch(e -> e instanceof com.ieltspath.game.application.result.RoomClosedEvent closed
                && closed.message().contains("Player")));
    }

    @Test void aDroppedPlayerKeepsTheirPlaceForThirtySeconds() {
        var published = recordEvents();
        List<UUID> guests = new java.util.ArrayList<>(java.util.Arrays.asList((UUID) null));
        UUID match = startMultiplayer(guests);
        UUID guest = guests.get(0);
        gameplay.current(match, user);
        gameplay.disconnected(match, guest);
        assertEquals("DISCONNECTED", sql.queryForObject("SELECT status FROM game_match_players WHERE match_id=? AND user_id=?",
                String.class, match, guest));
        assertTrue(published.getLast() instanceof com.ieltspath.game.application.result.RocketRoundEvents.RankingChanged);

        now = now.plusSeconds(10);
        gameplay.reconnected(match, guest);
        assertEquals("ACTIVE", sql.queryForObject("SELECT status FROM game_match_players WHERE match_id=? AND user_id=?",
                String.class, match, guest));

        gameplay.disconnected(match, guest);
        assertTrue(store.dueMatches(now.plusSeconds(31), 100).contains(match), "the worker picks up an expired grace");
        now = now.plusSeconds(31);
        gameplay.advance(match);
        assertEquals("FORFEITED", sql.queryForObject("SELECT status FROM game_match_players WHERE match_id=? AND user_id=?",
                String.class, match, guest));
        assertEquals("LEFT", memberStatus(match, guest));
        assertTrue(gameplay.current(match, user).matchFinished());
    }

    @Test void soloMatchesIgnoreConnectionChanges() {
        UUID match = start(3);
        gameplay.disconnected(match, user);
        assertEquals("ACTIVE", sql.queryForObject("SELECT status FROM game_match_players WHERE match_id=?", String.class, match));
    }

    @Test void outsidersCannotSeeOrAnswer() {
        UUID match = start(3);
        var round = gameplay.current(match, user).round();
        UUID stranger = UUID.randomUUID();
        assertThrows(GameMatchNotFoundException.class, () -> gameplay.current(match, stranger));
        assertThrows(GameMatchNotFoundException.class, () -> gameplay.answer(match, round.roundId(), stranger, "x", wrong(match, round)));
        assertThrows(GameMatchNotFoundException.class, () -> gameplay.current(UUID.randomUUID(), user));
        assertCode("ROUND_NOT_FOUND", () -> gameplay.answer(match, UUID.randomUUID(), user, "x", wrong(match, round)));
        assertCode("INVALID_OPTION", () -> gameplay.answer(match, round.roundId(), user, "x", UUID.randomUUID()));
        assertEquals(RocketRound.Status.OPEN.name(),
                sql.queryForObject("SELECT status FROM game_rocket_rounds WHERE id=?", String.class, round.roundId()));
    }

    @Test void concurrentSchedulesCloseAnExpiredRoundOnlyOnce() throws Exception {
        UUID match = start(3);
        now = now.plusSeconds(16);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var barrier = new java.util.concurrent.CyclicBarrier(2);
        try {
            var runs = java.util.stream.Stream.of(1, 2).map(i -> pool.submit(() -> {
                barrier.await();
                gameplay.advance(match);
                return null;
            })).toList();
            for (var run : runs) run.get(30, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        long closes = published.stream()
                .filter(event -> event instanceof RocketRoundEvents.Closed).count();
        assertEquals(1, closes);
        assertEquals(1, sql.queryForObject(
                "SELECT count(*) FROM game_rocket_rounds WHERE match_id=? AND status='CLOSED'", Integer.class, match));
    }
}
