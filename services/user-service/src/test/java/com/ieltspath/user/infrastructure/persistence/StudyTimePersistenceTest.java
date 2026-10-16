package com.ieltspath.user.infrastructure.persistence;

import com.ieltspath.user.application.usecase.RecordStudyHeartbeatUseCase;
import com.ieltspath.user.domain.aggregate.StudySession;
import com.ieltspath.user.infrastructure.adapter.StudySessionRepositoryAdapter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker=true)
class StudyTimePersistenceTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;
    static StudySessionRepositoryAdapter sessions;
    static TransactionTemplate tx;
    @BeforeAll static void setup() {
        var ds=new DriverManagerDataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        jdbc=new JdbcTemplate(ds); sessions=new StudySessionRepositoryAdapter(jdbc);
        tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
    }
    private UUID user() {
        UUID user=UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,email,full_name,password_hash,status) VALUES (?,?,'Study learner','test-only','ACTIVE')",user,user+"@example.test");
        return user;
    }
    @Test void simultaneousReplayCommitsOneLedgerAndOneTotal() throws Exception {
        UUID user=user(),course=UUID.randomUUID();
        Instant anchor=Instant.parse("2026-10-14T16:59:50Z");
        var session=StudySession.start(user,UUID.randomUUID(),"LESSON",UUID.randomUUID(),course,"Asia/Ho_Chi_Minh",anchor);
        tx.executeWithoutResult(s->sessions.save(session));
        var heartbeat=new RecordStudyHeartbeatUseCase(sessions,Clock.fixed(anchor.plusSeconds(30),ZoneOffset.UTC));
        var barrier=new CyclicBarrier(2);
        Callable<Long> record=()-> {
            barrier.await(5,TimeUnit.SECONDS);
            return tx.execute(s->heartbeat.execute(user,session.id(),1,1,List.of(new StudySession.Interval(0,30000)),false).acceptedMs());
        };
        try(var pool=Executors.newFixedThreadPool(2)) {
            var first=pool.submit(record); var second=pool.submit(record);
            assertEquals(30000L,first.get(10,TimeUnit.SECONDS));
            assertEquals(30000L,second.get(10,TimeUnit.SECONDS));
        }
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM study_heartbeat_receipts WHERE session_id=?",Integer.class,session.id()));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM study_activity_intervals WHERE session_id=?",Integer.class,session.id()));
        assertEquals(30000L,jdbc.queryForObject("SELECT sum(active_ms) FROM study_daily_totals WHERE user_id=? AND scope_key='ALL'",Long.class,user));
        assertEquals(30000L,jdbc.queryForObject("SELECT sum(active_ms) FROM study_daily_totals WHERE user_id=? AND scope_key=?",Long.class,user,course.toString()));
    }
    @Test void receiptAndTotalsRollbackTogether() {
        UUID user=user(); Instant now=Instant.now();
        var session=StudySession.start(user,UUID.randomUUID(),"LESSON",UUID.randomUUID(),null,"UTC",now.minusSeconds(30));
        tx.executeWithoutResult(s->sessions.save(session));
        var heartbeat=new RecordStudyHeartbeatUseCase(sessions,Clock.fixed(now,ZoneOffset.UTC));
        tx.executeWithoutResult(s->{
            heartbeat.execute(user,session.id(),1,1,List.of(new StudySession.Interval(0,30000)),false);
            s.setRollbackOnly();
        });
        assertTrue(sessions.receipt(session.id(),1,1).isEmpty());
        assertEquals(0,sessions.find(user,session.id()).orElseThrow().totalAcceptedMs());
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM study_daily_totals WHERE user_id=?",Integer.class,user));
    }

    @Test void subMillisecondMidnightHeartbeatPersistsAndReplaysOnce() {
        UUID user=user();
        Instant anchor=Instant.parse("2026-10-14T16:59:59.999500Z");
        var session=StudySession.start(user,UUID.randomUUID(),"LESSON",UUID.randomUUID(),null,"Asia/Ho_Chi_Minh",anchor);
        tx.executeWithoutResult(status->sessions.save(session));
        var heartbeat=new RecordStudyHeartbeatUseCase(sessions,Clock.fixed(anchor.plusMillis(1),ZoneOffset.UTC));
        var interval=List.of(new StudySession.Interval(0,1));

        var first=tx.execute(status->heartbeat.execute(user,session.id(),1,1,interval,false));
        var replay=tx.execute(status->heartbeat.execute(user,session.id(),1,1,interval,false));

        assertEquals(1,first.acceptedMs());
        assertEquals(first,replay);
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM study_heartbeat_receipts WHERE session_id=?",
                Integer.class,session.id()));
        assertEquals(1L,jdbc.queryForObject("SELECT sum(active_ms) FROM study_activity_intervals WHERE session_id=?",
                Long.class,session.id()));
        assertEquals(1L,jdbc.queryForObject("SELECT sum(active_ms) FROM study_daily_totals WHERE user_id=?",
                Long.class,user));
    }
    @Test void migrationEnforcesSingleOpenLeaseAndBoundedQueriesReturnHistoricalGoal() {
        UUID user=user(); Instant now=Instant.now();
        var first=StudySession.start(user,UUID.randomUUID(),"LESSON",UUID.randomUUID(),null,"UTC",now);
        sessions.save(first);
        var second=StudySession.start(user,UUID.randomUUID(),"LESSON",UUID.randomUUID(),null,"UTC",now);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->sessions.save(second));
        LocalDate date=LocalDate.parse("2026-10-14");
        sessions.scheduleGoal(user,UUID.randomUUID(),30,date,"UTC",date.atStartOfDay(ZoneOffset.UTC).toInstant());
        assertEquals(30,sessions.goals(user,date.plusDays(1),date.plusDays(7)).getFirst().minutes());
    }
}
