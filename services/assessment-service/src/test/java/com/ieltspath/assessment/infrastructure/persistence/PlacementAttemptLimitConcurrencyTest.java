package com.ieltspath.assessment.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.command.StartAssessmentAttemptCommand;
import com.ieltspath.assessment.application.port.ContentPackageProvider;
import com.ieltspath.assessment.application.usecase.AttemptCreator;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.exception.PlacementLimitReachedException;
import com.ieltspath.assessment.domain.repository.*;
import com.ieltspath.assessment.domain.vo.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
class PlacementAttemptLimitConcurrencyTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void onlyOneConcurrentStartCanCrossThePerLearnerLimitBoundary() throws Exception {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID learner = UUID.randomUUID();
        for (int i = 0; i < 2; i++) jdbc.update("INSERT INTO assessment_attempts(id,user_id,package_version_id,attempt_type,mode,channel,status) " +
                "VALUES(?,?,?,'PLACEMENT','STANDARD','WEB','SUBMITTED')", UUID.randomUUID(), learner, UUID.randomUUID());

        AssessmentAttemptRepository attempts = mock(AssessmentAttemptRepository.class);
        when(attempts.countPlacementAttemptsSince(eq(learner), any())).thenAnswer(invocation -> jdbc.queryForObject(
                "SELECT count(*) FROM assessment_attempts WHERE user_id=? AND attempt_type='PLACEMENT' AND created_at>=?",
                Long.class, learner, Timestamp.from((Instant) invocation.getArgument(1))));
        when(attempts.save(any())).thenAnswer(invocation -> {
            AssessmentAttempt attempt = invocation.getArgument(0);
            jdbc.update("INSERT INTO assessment_attempts(id,user_id,package_version_id,attempt_type,mode,channel,status,started_at,created_at,updated_at,placement_pool) " +
                            "VALUES(?,?,?,'PLACEMENT','STANDARD','WEB','IN_PROGRESS',?,?,?,?)", attempt.getId(), learner,
                    attempt.getPackageVersionId(), Timestamp.from(attempt.getStartedAt()), Timestamp.from(attempt.getCreatedAt()),
                    Timestamp.from(attempt.getUpdatedAt()),
                    attempt.getPlacementPool().name());
            return attempt;
        });
        var creator = new AttemptCreator(attempts, mock(AttemptSectionRepository.class), mock(AttemptItemRepository.class),
                mock(AttemptItemKnowledgePointRepository.class), new ObjectMapper(), new JdbcTemplate(dataSource), 3);
        var tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        var version = packageVersion();
        var command = new StartAssessmentAttemptCommand(learner, version.packageVersionId(), AttemptMode.STANDARD,
                AttemptChannel.WEB, List.of());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> outcomes = new ArrayList<>();
            for (int i = 0; i < 2; i++) outcomes.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try { tx.execute(status -> creator.create(command, AttemptType.PLACEMENT, version)); return true; }
                catch (PlacementLimitReachedException expected) { return false; }
            }));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            go.countDown();
            assertEquals(1, outcomes.stream().filter(f -> get(f)).count());
            assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM assessment_attempts WHERE user_id=? AND attempt_type='PLACEMENT'", Integer.class, learner));
        } finally {
            pool.shutdownNow();
        }
    }

    private static boolean get(Future<Boolean> future) {
        try { return future.get(10, TimeUnit.SECONDS); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    private static ContentPackageProvider.PackageVersion packageVersion() {
        var item = new ContentPackageProvider.Item(UUID.randomUUID(), 1, "question", List.of(),
                Map.of("type", "CHOICE", "correct", "A"), null, BigDecimal.ONE, List.of());
        var section = new ContentPackageProvider.Section(UUID.randomUUID(), "title", "READING", null, 1, null,
                null, List.of(item));
        return new ContentPackageProvider.PackageVersion(UUID.randomUUID(), UUID.randomUUID(), "PLACEMENT_TEST",
                List.of(section), PlacementPool.BAND_0_3);
    }
}
