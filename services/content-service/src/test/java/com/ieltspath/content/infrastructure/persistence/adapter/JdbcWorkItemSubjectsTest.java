package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.result.WorkItemSubjectsResult;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class JdbcWorkItemSubjectsTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static NamedParameterJdbcTemplate jdbc;
    private static JdbcLearningContentReader reader;

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        jdbc = new NamedParameterJdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        reader = new JdbcLearningContentReader(jdbc);
    }

    @Test
    void knowledgePointsAreNamedByCodeAndName() {
        Map<String, Object> kp = jdbc.queryForMap("SELECT id, code, name FROM knowledge_points LIMIT 1", Map.of());
        UUID id = (UUID) kp.get("id");

        WorkItemSubjectsResult result = reader.workItemSubjects(List.of(id, UUID.randomUUID()), List.of());

        assertEquals(1, result.knowledgePoints().size());
        assertEquals(kp.get("code"), result.knowledgePoints().get(id).code());
        assertEquals(kp.get("name"), result.knowledgePoints().get(id).name());
        assertTrue(result.questions().isEmpty());
    }

    @Test
    void questionsCarryTheirQuestionIdAndAStemCutAtOneHundredTwentyCharacters() {
        UUID questionId = UUID.randomUUID();
        jdbc.update("INSERT INTO questions (id, question_type, skill, purpose, status) "
                + "VALUES (:id, 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'DRAFT')", Map.of("id", questionId));
        UUID versionId = UUID.randomUUID();
        jdbc.update("INSERT INTO question_versions (id, question_id, version_number, stem) "
                        + "VALUES (:id, :question, 1, :stem)",
                Map.of("id", versionId, "question", questionId, "stem", "x".repeat(300)));

        WorkItemSubjectsResult result = reader.workItemSubjects(List.of(), List.of(versionId, UUID.randomUUID()));

        assertEquals(1, result.questions().size());
        assertEquals(questionId, result.questions().get(versionId).questionId());
        assertEquals(120, result.questions().get(versionId).stemPreview().length());
        assertTrue(result.knowledgePoints().isEmpty());
    }

    @Test
    void emptyRequestsReadNothing() {
        WorkItemSubjectsResult result = reader.workItemSubjects(List.of(), List.of());

        assertTrue(result.knowledgePoints().isEmpty());
        assertTrue(result.questions().isEmpty());
    }
}
