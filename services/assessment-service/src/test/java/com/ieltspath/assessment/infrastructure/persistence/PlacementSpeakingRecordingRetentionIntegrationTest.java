package com.ieltspath.assessment.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ieltspath.assessment.application.usecase.PurgeExpiredSpeakingRecordingsUseCase;
import com.ieltspath.assessment.config.SpeakingRecordingProperties;
import com.ieltspath.assessment.infrastructure.persistence.adapter.JdbcPlacementSpeakingGradeStore;
import com.ieltspath.assessment.infrastructure.persistence.adapter.JdbcSpeakingRecordingStore;
import com.ieltspath.commonobservability.OutboxTraceContext;
import com.ieltspath.commonstorage.ObjectStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Testcontainers(disabledWithoutDocker = true)
class PlacementSpeakingRecordingRetentionIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");

    private JdbcTemplate jdbc;
    private JdbcSpeakingRecordingStore recordings;
    private JdbcPlacementSpeakingGradeStore grades;
    private ObjectStorage storage;

    @BeforeEach
    void setUp() {
        var source = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(source);
        jdbc.execute("DROP TABLE IF EXISTS placement_speaking_grades");
        jdbc.execute("DROP TABLE IF EXISTS learner_submissions");
        jdbc.execute("DROP TABLE IF EXISTS attempt_items");
        jdbc.execute("DROP TABLE IF EXISTS attempt_sections");
        jdbc.execute("DROP TABLE IF EXISTS assessment_attempts");
        jdbc.execute("CREATE TABLE assessment_attempts (id UUID PRIMARY KEY, attempt_type VARCHAR(30) NOT NULL)");
        jdbc.execute("CREATE TABLE attempt_sections (id UUID PRIMARY KEY, attempt_id UUID NOT NULL REFERENCES assessment_attempts(id))");
        jdbc.execute("CREATE TABLE attempt_items (id UUID PRIMARY KEY, attempt_section_id UUID NOT NULL REFERENCES attempt_sections(id))");
        jdbc.execute("""
                CREATE TABLE learner_submissions (
                    id UUID PRIMARY KEY, user_id UUID NOT NULL, attempt_item_id UUID NOT NULL REFERENCES attempt_items(id),
                    skill VARCHAR(20) NOT NULL,
                    audio_reference TEXT NOT NULL, submitted_at TIMESTAMPTZ NOT NULL,
                    recording_deleted_at TIMESTAMPTZ
                )
                """);
        jdbc.execute("""
                CREATE TABLE placement_speaking_grades (
                    attempt_id UUID PRIMARY KEY REFERENCES assessment_attempts(id), user_id UUID NOT NULL,
                    status VARCHAR(16) NOT NULL,
                    band NUMERIC(3,1), feedback JSONB, rubric_code VARCHAR(100), rubric_version INTEGER,
                    failure_code VARCHAR(80)
                )
                """);
        var namedJdbc = new NamedParameterJdbcTemplate(source);
        recordings = new JdbcSpeakingRecordingStore(namedJdbc);
        grades = new JdbcPlacementSpeakingGradeStore(jdbc,
                new ObjectMapper().registerModule(new JavaTimeModule()), mock(OutboxTraceContext.class));
        storage = mock(ObjectStorage.class);
    }

    @Test
    void placementRecordingIsDeletedAfterNinetyDaysAndItsGradeAndFeedbackRemain() {
        UUID userId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        String objectKey = "speaking/" + userId + "/" + UUID.randomUUID() + ".wav";
        jdbc.update("INSERT INTO assessment_attempts(id,attempt_type) VALUES (?,'PLACEMENT')", attemptId);
        jdbc.update("INSERT INTO attempt_sections(id,attempt_id) VALUES (?,?)", sectionId, attemptId);
        jdbc.update("INSERT INTO attempt_items(id,attempt_section_id) VALUES (?,?)", itemId, sectionId);
        jdbc.update("""
                INSERT INTO learner_submissions(id,user_id,attempt_item_id,skill,audio_reference,submitted_at)
                VALUES (?,?,?, 'SPEAKING', ?, ?)
                """, submissionId, userId, itemId, objectKey,
                java.sql.Timestamp.from(NOW.minusSeconds(91L * 86_400)));
        jdbc.update("""
                INSERT INTO placement_speaking_grades(attempt_id,user_id,status,band,feedback)
                VALUES (?,?,'GRADED',6.5,'{"summary":"Clear answers","criteria":[]}')
                """, attemptId, userId);
        PurgeExpiredSpeakingRecordingsUseCase purge = new PurgeExpiredSpeakingRecordingsUseCase(
                recordings, storage, new SpeakingRecordingProperties("speaking-recordings", 90, 900));

        assertEquals(1, purge.execute(NOW));

        verify(storage).delete("speaking-recordings", objectKey);
        assertNotNull(jdbc.queryForObject("SELECT recording_deleted_at FROM learner_submissions WHERE id=?",
                java.sql.Timestamp.class, submissionId));
        var grade = grades.find(attemptId).orElseThrow();
        assertEquals("GRADED", grade.status());
        assertEquals(6.5, grade.band());
        assertEquals("{\"summary\": \"Clear answers\", \"criteria\": []}", grade.feedback());
    }
}
