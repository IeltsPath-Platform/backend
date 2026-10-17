package com.ieltspath.assessment.infrastructure.persistence;

import com.ieltspath.assessment.domain.vo.AttemptScope;
import com.ieltspath.assessment.domain.vo.Skill;
import com.ieltspath.assessment.infrastructure.persistence.adapter.JdbcPracticeAttemptReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
class JdbcPracticeAttemptReaderTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void progressAndHistoryAreScopedToUserAndPackageAcrossVersions() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        JdbcPracticeAttemptReader reader = new JdbcPracticeAttemptReader(new NamedParameterJdbcTemplate(dataSource));

        UUID packageId = UUID.randomUUID();
        UUID learner = UUID.randomUUID();
        UUID otherLearner = UUID.randomUUID();
        UUID partAttempt = insertAttempt(jdbc, learner, packageId, UUID.randomUUID(), "PART", "SUBMITTED",
                Instant.parse("2026-10-01T10:00:00Z"));
        UUID partItem = insertSectionAndItem(jdbc, partAttempt, 2);
        UUID fullAttempt = insertAttempt(jdbc, learner, packageId, UUID.randomUUID(), "FULL", "SUBMITTED",
                Instant.parse("2026-10-02T10:00:00Z"));
        UUID fullSection1 = insertSection(jdbc, fullAttempt, 1);
        insertSection(jdbc, fullAttempt, 2);
        insertSection(jdbc, fullAttempt, 3);
        UUID fullItem = insertItem(jdbc, fullSection1);
        insertCompletedResult(jdbc, fullAttempt, fullItem, 1, 1, 1, "COMPLETED");
        insertCompletedResult(jdbc, fullAttempt, fullItem, 2, 1, 2, "COMPLETED");
        insertCompletedResult(jdbc, fullAttempt, fullItem, 3, 2, 2, "PROCESSING");
        UUID otherAttempt = insertAttempt(jdbc, otherLearner, packageId, UUID.randomUUID(), "PART", "SUBMITTED",
                Instant.parse("2026-10-03T10:00:00Z"));
        insertSectionAndItem(jdbc, otherAttempt, 3);
        insertAttempt(jdbc, learner, UUID.randomUUID(), UUID.randomUUID(), "PART", "IN_PROGRESS",
                Instant.parse("2026-10-04T10:00:00Z"));

        var fullProgress = reader.findProgress(learner, Skill.READING, AttemptScope.FULL, List.of());
        var partProgress = reader.findProgress(learner, Skill.READING, AttemptScope.PART, List.of());
        var history = reader.findHistory(learner, Skill.READING, 0, 10);

        assertEquals(fullProgress, reader.findProgress(learner, Skill.READING, AttemptScope.FULL, List.of(packageId)));
        assertEquals(List.of(), reader.findProgress(learner, Skill.READING, AttemptScope.FULL,
                List.of(UUID.randomUUID())));
        assertEquals(partProgress, reader.findProgress(learner, Skill.READING, AttemptScope.PART, List.of(packageId)));
        assertEquals(0, reader.findHistory(learner, Skill.READING, 2_000_000, 50).items().size());

        assertEquals(List.of(new com.ieltspath.assessment.application.port.PracticeAttemptReader.ProgressKey(
                packageId, null)), fullProgress);
        assertEquals(List.of(1, 2, 3), partProgress.stream().map(
                com.ieltspath.assessment.application.port.PracticeAttemptReader.ProgressKey::partNo).toList());
        assertEquals(2, history.totalElements());
        assertEquals(fullAttempt, history.items().getFirst().attemptId());
        assertEquals(List.of(1, 2, 3), history.items().getFirst().partNos());
        assertEquals(50.0, history.items().getFirst().percent());
        assertEquals(AttemptScope.FULL, history.items().getFirst().scope());
    }

    private static UUID insertAttempt(JdbcTemplate jdbc, UUID userId, UUID packageId, UUID versionId, String scope,
                                      String status, Instant submittedAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO assessment_attempts (id, user_id, package_version_id, package_id, attempt_type, mode,
                    channel, status, started_at, submitted_at, scope, skill)
                VALUES (?, ?, ?, ?, 'OFFICIAL_PRACTICE', 'STANDARD', 'WEB', ?, ?, ?, ?, 'READING')
                """, id, userId, versionId, packageId, status, java.sql.Timestamp.from(submittedAt.minusSeconds(600)),
                "SUBMITTED".equals(status) ? java.sql.Timestamp.from(submittedAt) : null, scope);
        return id;
    }

    private static UUID insertSectionAndItem(JdbcTemplate jdbc, UUID attemptId, int partNo) {
        return insertItem(jdbc, insertSection(jdbc, attemptId, partNo));
    }

    private static UUID insertSection(JdbcTemplate jdbc, UUID attemptId, int partNo) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO attempt_sections (id, attempt_id, content_section_id, sort_order, section_snapshot, part_no)
                VALUES (?, ?, ?, ?, '{}', ?)
                """, id, attemptId, UUID.randomUUID(), partNo, partNo);
        return id;
    }

    private static UUID insertItem(JdbcTemplate jdbc, UUID sectionId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO attempt_items (id, attempt_section_id, question_version_id, sort_order, question_snapshot)
                VALUES (?, ?, ?, 1, '{}')
                """, id, sectionId, UUID.randomUUID());
        return id;
    }

    private static void insertCompletedResult(JdbcTemplate jdbc, UUID attemptId, UUID itemId, int version,
                                              double score, double maxScore, String status) {
        UUID resultId = UUID.randomUUID();
        jdbc.update("INSERT INTO assessment_results (id, attempt_id, result_version, status) VALUES (?, ?, ?, ?)",
                resultId, attemptId, version, status);
        if ("COMPLETED".equals(status)) {
            jdbc.update("""
                    INSERT INTO item_results (id, result_id, attempt_item_id, score, max_score, is_correct,
                        feedback_snapshot)
                    VALUES (?, ?, ?, ?, ?, ?, '{}')
                    """, UUID.randomUUID(), resultId, itemId, score, maxScore, score == maxScore);
        }
    }
}
