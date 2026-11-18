package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.PlacementPicker;
import com.ieltspath.learning.application.usecase.CreatePlacementAssignment;
import com.ieltspath.learning.domain.vo.PlacementPool;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class PlacementAssignmentIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    static DataSource dataSource;
    static JdbcTemplate jdbc;
    static JdbcPlacementAssignmentRepository assignments;
    static TransactionTemplate transactions;

    @BeforeAll static void migrate() {
        dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        assignments = new JdbcPlacementAssignmentRepository(new NamedParameterJdbcTemplate(dataSource));
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Test void migrationChecksPoolAndRollingLimitExcludesAnAssignmentAtTheExactBoundary() {
        UUID user = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        insert(user, PlacementPool.BAND_3_5.name(), now.minusSeconds(24 * 60 * 60), null);
        assertThat(assignments.countSince(user, now.minusSeconds(24 * 60 * 60))).isZero();
        assertThatThrownBy(() -> jdbc.update("UPDATE placement_assignments SET pool='UNKNOWN' WHERE user_id=?", user))
                .isInstanceOf(DataAccessException.class);
    }

    @Test void releasedAssignmentIsNeitherCountedNorUsedForRotation() {
        UUID user = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        UUID released = insert(user, PlacementPool.BAND_3_5.name(), now.minusSeconds(100), now.minusSeconds(10));
        assertThat(assignments.countSince(user, now.minusSeconds(24 * 60 * 60))).isZero();
        assertThat(assignments.latestByPackage(user)).doesNotContainKey(released);
    }

    @Test void concurrentAssignmentsForOneLearnerReuseTheFirstAssignmentUnderTheAdvisoryLock() throws Exception {
        UUID user = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        var lock = new JdbcLearnerLock(new JdbcTemplate(dataSource));
        PlacementPicker picker = bound -> 0;
        var creator = new CreatePlacementAssignment(assignments, lock, picker);
        var candidate = new LearningContentClient.PlacementPackage(UUID.randomUUID(), UUID.randomUUID(), "test",
                PlacementPool.BAND_3_5);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> transactions.execute(status -> creator.create(user,
                    PlacementPool.BAND_3_5, List.of(candidate), now, now.minusSeconds(900),
                    now.minusSeconds(24 * 60 * 60), 3)));
            var second = executor.submit(() -> transactions.execute(status -> creator.create(user,
                    PlacementPool.BAND_3_5, List.of(candidate), now, now.minusSeconds(900),
                    now.minusSeconds(24 * 60 * 60), 3)));
            assertThat(first.get(30, TimeUnit.SECONDS).id()).isEqualTo(second.get(30, TimeUnit.SECONDS).id());
            assertThat(assignments.countSince(user, now.minusSeconds(24 * 60 * 60))).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private static UUID insert(UUID user, String pool, Instant assignedAt, Instant releasedAt) {
        UUID packageId = UUID.randomUUID();
        jdbc.update("INSERT INTO placement_assignments(id,user_id,package_id,package_version_id,pool,assigned_at,released_at) " +
                        "VALUES(?,?,?,?,?,?,?)", UUID.randomUUID(), user, packageId, UUID.randomUUID(), pool,
                Timestamp.from(assignedAt), releasedAt == null ? null : Timestamp.from(releasedAt));
        return packageId;
    }
}
