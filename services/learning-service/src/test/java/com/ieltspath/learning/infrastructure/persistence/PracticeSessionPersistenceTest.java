package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.usecase.OpenPracticeSessionUseCase;
import com.ieltspath.learning.application.usecase.RecordStudyHeartbeatUseCase;
import com.ieltspath.learning.domain.aggregate.StudySession;
import com.ieltspath.learning.infrastructure.adapter.PracticeSessionJdbcAdapter;
import com.ieltspath.learning.infrastructure.adapter.StudySessionRepositoryAdapter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
class PracticeSessionPersistenceTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;
    static PracticeSessionJdbcAdapter practiceSessions;
    static StudySessionRepositoryAdapter studySessions;
    static TransactionTemplate transactions;
    static Clock clock;

    @BeforeAll
    static void setup() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        practiceSessions = new PracticeSessionJdbcAdapter(jdbc);
        studySessions = new StudySessionRepositoryAdapter(jdbc);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        clock = Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC);
    }

    @Test
    void writingPracticeIsIdempotentAndAddsOnlyToTheAllStudyTotal() {
        UUID user = UUID.randomUUID(), requestId = UUID.randomUUID();
        var open = new OpenPracticeSessionUseCase(practiceSessions, new JdbcLearnerLock(jdbc), transactions, clock);
        var first = open.execute(user, "WRITING", requestId);
        var replay = open.execute(user, "WRITING", requestId);
        assertEquals(first.id(), replay.id());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM practice_sessions WHERE user_id=?", Integer.class, user));

        var study = StudySession.start(user, UUID.randomUUID(), "WRITING", first.id(), null, "UTC", clock.instant());
        studySessions.save(study);
        var heartbeat = new RecordStudyHeartbeatUseCase(studySessions, Clock.fixed(clock.instant().plusSeconds(30), ZoneOffset.UTC));
        transactions.execute(status -> heartbeat.execute(user, study.id(), 1, 1,
                List.of(new StudySession.Interval(0, 30_000)), false));

        assertEquals(30_000L, jdbc.queryForObject(
                "SELECT active_ms FROM study_daily_totals WHERE user_id=? AND scope_key='ALL'", Long.class, user));
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM study_daily_totals WHERE user_id=? AND scope_key<>'ALL'", Integer.class, user));
        assertEquals(30_000L, new JdbcLearningOverviewReader(jdbc).totals(user).studyMilliseconds());
    }
}
