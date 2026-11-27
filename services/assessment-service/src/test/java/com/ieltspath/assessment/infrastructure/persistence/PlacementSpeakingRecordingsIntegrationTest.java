package com.ieltspath.assessment.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.PlacementSpeakingGradeStore;
import com.ieltspath.assessment.infrastructure.persistence.adapter.JdbcPlacementSpeakingGradeStore;
import com.ieltspath.commonobservability.OutboxTraceContext;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Submitting a placement test reads its Speaking recordings, whether or not the learner recorded any, so this query has
 * to run against the real schema.
 */
@Testcontainers(disabledWithoutDocker = true)
class PlacementSpeakingRecordingsIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;
    static PlacementSpeakingGradeStore grades;

    @BeforeAll static void setup() {
        var source = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(source).load().migrate();
        jdbc = new JdbcTemplate(source);
        grades = new JdbcPlacementSpeakingGradeStore(jdbc, new ObjectMapper(), mock(OutboxTraceContext.class));
    }

    @Test void returnsTheRecordingsOfTheSpeakingSectionsOnly() {
        UUID user = UUID.randomUUID();
        UUID attempt = placementAttempt(user);
        UUID speaking = item(section(attempt, 0, "{\"skill\":\"SPEAKING\",\"partNo\":2}"));
        UUID reading = item(section(attempt, 1, "{\"skill\":\"READING\",\"partNo\":1}"));
        recording(user, speaking, "speaking/one.wav");
        recording(user, reading, "speaking/not-a-speaking-section.wav");

        assertEquals(List.of(new PlacementSpeakingGradeStore.RecordingItem(speaking, 2, 1, "Describe your town.",
                "speaking/one.wav")), grades.recordings(attempt, user));
    }

    @Test void usesTheSectionOrderWhenTheSectionHasNoPartNumber() {
        UUID user = UUID.randomUUID();
        UUID attempt = placementAttempt(user);
        UUID speaking = item(section(attempt, 2, "{\"skill\":\"speaking\",\"partNo\":null}"));
        recording(user, speaking, "speaking/two.wav");

        assertEquals(3, grades.recordings(attempt, user).getFirst().partNo());
    }

    @Test void isEmptyForAPlacementWithoutRecordings() {
        UUID user = UUID.randomUUID();
        UUID attempt = placementAttempt(user);
        item(section(attempt, 0, "{\"skill\":\"READING\",\"partNo\":1}"));

        assertEquals(List.of(), grades.recordings(attempt, user));
    }

    private static UUID placementAttempt(UUID user) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO assessment_attempts(id,user_id,package_version_id,attempt_type,mode,channel,status) "
                + "VALUES(?,?,?,'PLACEMENT','STANDARD','WEB','IN_PROGRESS')", id, user, UUID.randomUUID());
        return id;
    }

    private static UUID section(UUID attempt, int order, String snapshot) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO attempt_sections(id,attempt_id,content_section_id,sort_order,section_snapshot) "
                + "VALUES(?,?,?,?,CAST(? AS JSONB))", id, attempt, UUID.randomUUID(), order, snapshot);
        return id;
    }

    private static UUID item(UUID section) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO attempt_items(id,attempt_section_id,question_version_id,sort_order,question_snapshot) "
                + "VALUES(?,?,?,0,CAST(? AS JSONB))", id, section, UUID.randomUUID(),
                "{\"stem\":\"Describe your town.\"}");
        return id;
    }

    private static void recording(UUID user, UUID item, String objectKey) {
        jdbc.update("INSERT INTO learner_submissions(id,user_id,attempt_item_id,prompt_snapshot,skill,audio_reference,"
                        + "status,submission_key) VALUES(?,?,?,CAST('{}' AS JSONB),'SPEAKING',?,'SUBMITTED',?)",
                UUID.randomUUID(), user, item, objectKey, UUID.randomUUID().toString());
    }
}
