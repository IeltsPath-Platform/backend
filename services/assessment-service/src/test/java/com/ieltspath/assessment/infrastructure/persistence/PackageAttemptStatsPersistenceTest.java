package com.ieltspath.assessment.infrastructure.persistence;

import com.ieltspath.assessment.application.port.PackageAttemptStatsReader.PackageStats;
import com.ieltspath.assessment.infrastructure.persistence.adapter.JdbcPackageAttemptStatsReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class PackageAttemptStatsPersistenceTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;
    static JdbcPackageAttemptStatsReader reader;

    @BeforeAll
    static void setup() {
        var ds = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(ds);
        reader = new JdbcPackageAttemptStatsReader(new NamedParameterJdbcTemplate(ds));
    }

    private UUID attempt(UUID packageId, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO assessment_attempts(id, user_id, package_version_id, attempt_type, mode, channel, status,
                                                package_id, skill)
                VALUES(?, ?, ?, 'OFFICIAL_PRACTICE', 'STANDARD', 'WEB', ?, ?, 'READING')
                """, id, UUID.randomUUID(), UUID.randomUUID(), status, packageId);
        return id;
    }

    private void result(UUID attempt, int version, String status, String band) {
        jdbc.update("INSERT INTO assessment_results(id, attempt_id, result_version, status, overall_band) "
                + "VALUES(?, ?, ?, ?, CAST(? AS numeric))", UUID.randomUUID(), attempt, version, status, band);
    }

    @Test
    void countsAttemptsAndAveragesTheLatestCompletedBandOfEach() {
        UUID pkg = UUID.randomUUID();
        UUID first = attempt(pkg, "SUBMITTED");
        result(first, 1, "COMPLETED", "5.0");
        result(first, 2, "COMPLETED", "6.0"); // a regrade replaces the first result
        UUID second = attempt(pkg, "SUBMITTED");
        result(second, 1, "COMPLETED", "7.0");
        UUID ungraded = attempt(pkg, "SUBMITTED");
        result(ungraded, 1, "PROCESSING", null);
        attempt(pkg, "IN_PROGRESS");
        UUID other = UUID.randomUUID();
        attempt(other, "SUBMITTED");

        List<PackageStats> rows = reader.forPackages(List.of(pkg, other, UUID.randomUUID()));

        PackageStats stats = rows.stream().filter(row -> row.packageId().equals(pkg)).findFirst().orElseThrow();
        assertThat(stats.started()).isEqualTo(4);
        assertThat(stats.submitted()).isEqualTo(3);
        assertThat(stats.averageBand()).isEqualByComparingTo(new BigDecimal("6.5"));
        PackageStats untouched = rows.stream().filter(row -> row.packageId().equals(other)).findFirst().orElseThrow();
        assertThat(untouched.started()).isEqualTo(1);
        assertThat(untouched.averageBand()).isNull();
        assertThat(rows).hasSize(2);
    }

    @Test
    void anEmptyAskReturnsNothing() {
        assertThat(reader.forPackages(List.of())).isEmpty();
    }
}
