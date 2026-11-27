package com.ieltspath.learning.infrastructure;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.usecase.AiGradingReportUseCase;
import com.ieltspath.learning.domain.vo.AiGradingReport.ReportStatus;
import com.ieltspath.learning.domain.vo.AiGradingReport.SubmissionType;
import com.ieltspath.learning.infrastructure.persistence.JdbcAiGradingReportRepository;
import com.ieltspath.learning.infrastructure.persistence.JdbcDeletedUserRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class AiGradingReportIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    static JdbcTemplate jdbc;
    static JdbcAiGradingReportRepository repository;
    static JdbcDeletedUserRepository deletedUserRepository;
    static AiGradingReportUseCase useCase;

    static final UUID OWNER = UUID.fromString("30000000-0000-4000-8000-000000000001");
    static final UUID OTHER = UUID.fromString("30000000-0000-4000-8000-000000000002");
    static final UUID WRITING_ID = UUID.fromString("30000000-0000-4000-8000-000000000011");
    static final UUID SPEAKING_ID = UUID.fromString("30000000-0000-4000-8000-000000000012");
    static final Instant NOW = Instant.parse("2026-11-27T08:00:00Z");

    @BeforeAll
    static void initDb() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).load().migrate();
        var ds = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(ds);
        repository = new JdbcAiGradingReportRepository(new NamedParameterJdbcTemplate(jdbc));
        deletedUserRepository = new JdbcDeletedUserRepository(jdbc);
        useCase = new AiGradingReportUseCase(repository);
    }

    @BeforeEach
    void resetData() {
        jdbc.execute("TRUNCATE TABLE ai_grading_reports, lesson_writing_submissions, speaking_submissions CASCADE");
        jdbc.update("""
                INSERT INTO lesson_writing_submissions (
                    id, user_id, lesson_id, block_id, question_version_id, knowledge_point_ids,
                    request_id, essay_text, word_count, prompt_snapshot, status, point_cost,
                    grading_started_at, submitted_at
                ) VALUES (?, ?, ?, ?, ?, ARRAY[]::uuid[], ?, 'My essay text', 3, '{}'::jsonb, 'FAILED', 1, ?, ?)
                """,
                WRITING_ID, OWNER, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), Timestamp.from(NOW), Timestamp.from(NOW));
        jdbc.update("""
                INSERT INTO speaking_submissions (
                    id, user_id, request_id, package_version_id, scope, status, point_cost,
                    consent_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'PART', 'ABANDONED', 5, ?, ?, ?)
                """,
                SPEAKING_ID, OWNER, UUID.randomUUID(), UUID.randomUUID(),
                Timestamp.from(NOW), Timestamp.from(NOW), Timestamp.from(NOW));
    }

    @Test
    void validatesOwnershipIdempotencyListingAndUserDeletionCleanup() {
        var invalidReason = assertThrows(LearningRequestException.class,
                () -> useCase.report(OWNER, WRITING_ID, SubmissionType.WRITING, "   "));
        assertEquals("INVALID_REASON", invalidReason.getCode());

        var notFound = assertThrows(LearningRequestException.class,
                () -> useCase.report(OWNER, UUID.randomUUID(), SubmissionType.WRITING, "Sai tiêu chí"));
        assertEquals("SUBMISSION_NOT_FOUND", notFound.getCode());

        var notOwner = assertThrows(LearningRequestException.class,
                () -> useCase.report(OTHER, WRITING_ID, SubmissionType.WRITING, "Không phải bài của tôi"));
        assertEquals(403, notOwner.getStatus());
        assertEquals("NOT_OWNER", notOwner.getCode());

        var first = useCase.report(OWNER, WRITING_ID, SubmissionType.WRITING, "  Điểm Task Response chưa hợp lý  ");
        assertTrue(first.created());
        assertEquals(ReportStatus.OPEN, first.report().status());
        assertEquals("Điểm Task Response chưa hợp lý", first.report().reason());

        // Idempotent replay
        var replay = useCase.report(OWNER, WRITING_ID, SubmissionType.WRITING, "Lý do khác");
        assertFalse(replay.created());
        assertEquals(first.report().id(), replay.report().id());

        var speaking = useCase.report(OWNER, SPEAKING_ID, SubmissionType.SPEAKING, "Lỗi nhận diện phát âm");
        assertTrue(speaking.created());

        var allPage = useCase.list(null, null, 0, 20);
        assertEquals(2, allPage.totalElements());

        var writingOnly = useCase.list(ReportStatus.OPEN, SubmissionType.WRITING, 0, 20);
        assertEquals(1, writingOnly.totalElements());
        assertEquals(WRITING_ID, writingOnly.content().getFirst().submissionId());

        int deleted = deletedUserRepository.deleteBatchByUserId("ai_grading_reports", "user_id", OWNER, 500);
        assertEquals(2, deleted);
        assertEquals(0, useCase.list(null, null, 0, 20).totalElements());
    }
}
