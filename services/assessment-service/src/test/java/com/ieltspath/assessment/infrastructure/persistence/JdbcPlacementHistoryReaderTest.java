package com.ieltspath.assessment.infrastructure.persistence;

import com.ieltspath.assessment.application.usecase.GetPlacementHistoryUseCase;
import com.ieltspath.assessment.infrastructure.persistence.adapter.JdbcPlacementHistoryReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class JdbcPlacementHistoryReaderTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void listsOnlyTheLearnersSubmittedPlacementsNewestFirstWithTheLatestResult() {
        var dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        var jdbc = new JdbcTemplate(dataSource);
        var history = new GetPlacementHistoryUseCase(
                new JdbcPlacementHistoryReader(new NamedParameterJdbcTemplate(dataSource)));
        UUID learner = UUID.randomUUID();

        UUID graded = attempt(jdbc, learner, "PLACEMENT", "SUBMITTED", "BAND_3_5", "2026-11-01T10:00:00Z");
        result(jdbc, graded, 1, "COMPLETED", 4.0, false);
        result(jdbc, graded, 2, "COMPLETED", 4.5, true);
        UUID grading = attempt(jdbc, learner, "PLACEMENT", "SUBMITTED", "BAND_0_3", "2026-11-02T10:00:00Z");
        result(jdbc, grading, 1, "DRAFT", 2.0, false);
        attempt(jdbc, learner, "PLACEMENT", "CANCELLED", "BAND_0_3", "2026-11-03T10:00:00Z");
        attempt(jdbc, learner, "OFFICIAL_PRACTICE", "SUBMITTED", null, "2026-11-04T10:00:00Z");
        attempt(jdbc, UUID.randomUUID(), "PLACEMENT", "SUBMITTED", "BAND_5_9", "2026-11-05T10:00:00Z");

        var first = history.execute(learner, 0, 1);
        var second = history.execute(learner, 1, 1);

        assertThat(first.hasNext()).isTrue();
        assertThat(first.items()).singleElement().satisfies(item -> {
            assertThat(item.attemptId()).isEqualTo(grading);
            assertThat(item.graded()).isFalse();
            assertThat(item.overallBand()).isNull();
        });
        assertThat(second.hasNext()).isFalse();
        assertThat(second.items()).singleElement().satisfies(item -> {
            assertThat(item.attemptId()).isEqualTo(graded);
            assertThat(item.pool()).isEqualTo("BAND_3_5");
            assertThat(item.graded()).isTrue();
            assertThat(item.overallBand()).isEqualTo(4.5);
            assertThat(item.partial()).isTrue();
        });
    }

    private static UUID attempt(JdbcTemplate jdbc, UUID userId, String type, String status, String pool,
                                String submittedAt) {
        UUID id = UUID.randomUUID();
        Timestamp at = Timestamp.from(Instant.parse(submittedAt));
        // A practice attempt must name its package and skill; a placement attempt must not.
        boolean practice = "OFFICIAL_PRACTICE".equals(type);
        jdbc.update("""
                INSERT INTO assessment_attempts (id, user_id, package_version_id, package_id, skill, attempt_type,
                    mode, channel, status, started_at, submitted_at, placement_pool)
                VALUES (?, ?, ?, ?, ?, ?, 'STANDARD', 'WEB', ?, ?, ?, ?)
                """, id, userId, UUID.randomUUID(), practice ? UUID.randomUUID() : null,
                practice ? "READING" : null, type, status, at, "SUBMITTED".equals(status) ? at : null, pool);
        return id;
    }

    private static void result(JdbcTemplate jdbc, UUID attemptId, int version, String status, double band,
                               boolean partial) {
        jdbc.update("""
                INSERT INTO assessment_results (id, attempt_id, result_version, status, overall_band, partial)
                VALUES (?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), attemptId, version, status, band, partial);
    }
}
