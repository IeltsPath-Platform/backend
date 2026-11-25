package com.ieltspath.assessment.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class PlacementAttemptMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void migrationAddsPoolAndResultMetadataWithoutChangingLegacyRowsAndRejectsUnknownPools() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).target(MigrationVersion.fromVersion("23")).load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID legacy = UUID.randomUUID();
        jdbc.update("INSERT INTO assessment_attempts(id,user_id,package_version_id,attempt_type,mode,channel,status) " +
                "VALUES(?,?,?,'PLACEMENT','STANDARD','WEB','SUBMITTED')", legacy, UUID.randomUUID(), UUID.randomUUID());
        jdbc.update("INSERT INTO assessment_results(id,attempt_id,result_version,status) VALUES(?,?,1,'COMPLETED')",
                UUID.randomUUID(), legacy);

        Flyway.configure().dataSource(dataSource).load().migrate();

        assertNull(jdbc.queryForObject("SELECT placement_pool FROM assessment_attempts WHERE id=?", String.class, legacy));
        assertFalse(jdbc.queryForObject("SELECT partial FROM assessment_results WHERE attempt_id=?", Boolean.class, legacy));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE indexname='idx_assessment_attempt_placement_limit'", Integer.class));
        assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE assessment_attempts SET placement_pool='UNKNOWN' WHERE id=?", legacy));
        assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE assessment_results SET placement_pool='UNKNOWN'"));
    }
}
