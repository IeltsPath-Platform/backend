package com.ieltspath.assessment.infrastructure.persistence;

import com.ieltspath.assessment.application.port.BandStatisticsStore;
import com.ieltspath.assessment.application.usecase.GetBandHistoryUseCase;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.infrastructure.persistence.adapter.JdbcBandStatisticsStore;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@Testcontainers(disabledWithoutDocker = true)
class BandStatisticsPersistenceTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;
    static JdbcBandStatisticsStore store;
    private final UUID user = UUID.randomUUID();
    private final Instant from = Instant.parse("2026-10-01T00:00:00Z");
    private final Instant until = Instant.parse("2026-11-01T00:00:00Z");

    @BeforeAll
    static void setup() {
        var ds = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(ds);
        store = new JdbcBandStatisticsStore(new NamedParameterJdbcTemplate(ds));
    }

    private UUID attempt(UUID owner, Instant submitted, String kind) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO assessment_attempts(id, user_id, package_version_id, attempt_type, mode, channel, status, submitted_at)
            VALUES(?, ?, ?, ?, 'STANDARD', 'WEB', 'SUBMITTED', ?)
            """, id, owner, UUID.randomUUID(), kind, Timestamp.from(submitted));
        return id;
    }

    private UUID attempt(Instant submitted) {
        return attempt(user, submitted, "MOCK");
    }

    private UUID result(UUID attempt, int version, String status, Double band) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO assessment_results(id, attempt_id, result_version, status, completed_at) VALUES(?, ?, ?, ?, ?)",
                id, attempt, version, status, "COMPLETED".equals(status) ? Timestamp.from(from.plusSeconds(9000)) : null);
        if (band != null) {
            score(id, "LISTENING", band, "AUTO");
        }
        return id;
    }

    private void score(UUID result, String skill, Double band, String source) {
        jdbc.update("INSERT INTO skill_scores(result_id, skill, band, grading_source) VALUES(?, ?, ?, ?)",
                result, skill, band, source);
    }

    private BandStatisticsStore.Page history(BandStatisticsStore.Cursor cursor, int limit) {
        return store.history(user, from, until, "MOCK", "LISTENING", cursor, limit);
    }

    @Test
    void vietnamMorningBelongsToVietnamDateWithInclusiveStartAndExclusiveEnd() {
        Instant midnight = Instant.parse("2026-10-14T17:00:00Z");
        UUID start = attempt(midnight);
        UUID morning = attempt(Instant.parse("2026-10-14T23:30:00Z"));
        result(start, 1, "COMPLETED", 5.0);
        result(morning, 1, "COMPLETED", 6.0);
        result(attempt(midnight.minusSeconds(1)), 1, "COMPLETED", 4.0);
        result(attempt(Instant.parse("2026-10-15T17:00:00Z")), 1, "COMPLETED", 7.0);
        var useCase = new GetBandHistoryUseCase(store, mock(AssessmentAttemptRepository.class));
        LocalDate day = LocalDate.of(2026, 10, 15);
        var today = useCase.execute(user, day, day, "MOCK", "LISTENING", null, 100, "Asia/Ho_Chi_Minh");
        assertEquals(List.of(morning, start), today.points().stream().map(BandStatisticsStore.Point::attemptId).toList());
        assertEquals(5.0, today.baseline());
        assertEquals(6.0, today.latest());
        var previous = useCase.execute(user, day.minusDays(1), day.minusDays(1), "MOCK", "LISTENING", null, 100,
                "Asia/Ho_Chi_Minh");
        assertEquals(1, previous.points().size());
        assertNull(previous.delta());
    }

    @Test
    void newestCompletedReplacesOlderWhilePendingVersionRetainsResult() {
        UUID attempt = attempt(from.plusSeconds(3600));
        UUID initial = result(attempt, 1, "COMPLETED", 6.0);
        UUID pending = result(attempt, 2, "PROCESSING", 7.0);
        var before = history(null, 100).points().getFirst();
        assertEquals(initial, before.resultId());
        assertEquals(1, before.resultVersion());
        assertEquals(from.plusSeconds(9000), before.gradedAt());
        assertEquals(6.0, before.band());
        assertTrue(before.regradePending());
        jdbc.update("UPDATE assessment_results SET status = 'COMPLETED', completed_at = ? WHERE id = ?",
                Timestamp.from(from.plusSeconds(10000)), pending);
        var after = history(null, 100);
        assertEquals(1, after.points().size());
        assertEquals(pending, after.points().getFirst().resultId());
        assertEquals(7.0, after.latest());
        assertFalse(after.points().getFirst().regradePending());
    }

    @Test
    void excludesOtherOwnersKindsAndRangeWithoutRevivingOlderValidVersion() {
        UUID replaced = attempt(from.plusSeconds(3600));
        result(replaced, 1, "COMPLETED", 6.0);
        result(replaced, 2, "COMPLETED", 9.5);
        result(attempt(from.plusSeconds(7200)), 1, "COMPLETED", null);
        result(attempt(UUID.randomUUID(), from.plusSeconds(3600), "MOCK"), 1, "COMPLETED", 9.0);
        result(attempt(user, from.plusSeconds(3600), "OFFICIAL_PRACTICE"), 1, "COMPLETED", 9.0);
        result(attempt(until), 1, "COMPLETED", 9.0);
        result(attempt(from.minusSeconds(1)), 1, "COMPLETED", 9.0);
        assertTrue(history(null, 100).points().isEmpty());
        assertNull(history(null, 100).latest());
    }

    @Test
    void cursorPagesPreserveWholeRangeSummaryAndUseAttemptIdForTies() {
        Instant submitted = from.plusSeconds(3600);
        UUID first = attempt(submitted);
        UUID second = attempt(submitted);
        result(first, 1, "COMPLETED", 5.0);
        result(second, 1, "COMPLETED", 7.0);
        var page = history(null, 1);
        var point = page.points().getFirst();
        var tail = history(new BandStatisticsStore.Cursor(point.submittedAt(), point.attemptId()), 1);
        assertEquals(1, tail.points().size());
        assertNotEquals(point.attemptId(), tail.points().getFirst().attemptId());
        assertEquals(page.baseline(), tail.baseline());
        assertEquals(page.latest(), tail.latest());
        assertEquals(2, tail.eligibleCount());
    }

    @Test
    void overallRequiresFourValidSkillsOnSameCompletedResultAndRoundsToHalfBand() {
        UUID full = result(attempt(from.plusSeconds(3600)), 1, "COMPLETED", 6.0);
        score(full, "READING", 6.5, "AUTO");
        score(full, "WRITING", 7.0, "AI");
        score(full, "SPEAKING", 7.5, "HUMAN");
        UUID partial = result(attempt(from.plusSeconds(7200)), 1, "COMPLETED", 9.0);
        score(partial, "READING", 9.0, "AUTO");
        score(partial, "WRITING", 9.0, "HUMAN");
        UUID invalid = result(attempt(from.plusSeconds(8000)), 1, "COMPLETED", 6.0);
        score(invalid, "READING", 6.0, "AUTO");
        score(invalid, "WRITING", 6.0, "HUMAN");
        score(invalid, "SPEAKING", 9.5, "HUMAN");
        var overall = store.history(user, from, until, "MOCK", "OVERALL", null, 100);
        assertEquals(1, overall.points().size());
        assertEquals(full, overall.points().getFirst().resultId());
        assertEquals(7.0, overall.latest());
        assertEquals("AI,AUTO,HUMAN", overall.points().getFirst().gradingSource());
        assertEquals("GRADED", overall.points().getFirst().measurementQuality());
        score(partial, "SPEAKING", 9.0, "HUMAN");
        result(jdbc.queryForObject("SELECT attempt_id FROM assessment_results WHERE id = ?", UUID.class, full),
                2, "COMPLETED", 6.5);
        assertEquals(9.0, store.history(user, from, until, "MOCK", "OVERALL", null, 100).baseline());
    }
}
