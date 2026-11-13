package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** After every migration, each Writing error code has a model definition and the task it applies to. */
@Testcontainers(disabledWithoutDocker = true)
class WritingErrorCodeDefinitionsMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    @Test
    void everyActiveWritingCodeHasAModelDefinition() throws Exception {
        try (Connection connection = connection()) {
            assertEquals("0", scalar(connection, """
                    SELECT count(*)::text FROM error_types
                    WHERE status = 'ACTIVE' AND 'WRITING' = ANY(skills)
                      AND (model_definition IS NULL OR btrim(model_definition) = '')
                    """));
        }
    }

    @Test
    void theSharedObjectiveCodesAreNoLongerWritingCodesAndKeepTheirPoint() throws Exception {
        try (Connection connection = connection()) {
            assertFalse(Boolean.parseBoolean(scalar(connection,
                    "SELECT ('WRITING' = ANY(skills))::text FROM error_types WHERE code = 'SPELLING'")));
            assertFalse(Boolean.parseBoolean(scalar(connection,
                    "SELECT ('WRITING' = ANY(skills))::text FROM error_types WHERE code = 'PLURAL_FORM'")));
            assertEquals("LS_SPELL", scalar(connection, """
                    SELECT kp.code FROM error_types et JOIN knowledge_points kp ON kp.id = et.knowledge_point_id
                    WHERE et.code = 'SPELLING'
                    """));
        }
    }

    @Test
    void theNewWritingCodesExistAsApprovedWritingCodesWithTheirTasks() throws Exception {
        try (Connection connection = connection()) {
            for (String code : new String[] {"T1_IRRELEVANT_CONTENT", "DATA_LANGUAGE", "MISSING_VERB", "PERFECT_TENSE",
                    "RUN_ON_SENTENCE", "REFERENCE", "REPETITION", "INFORMAL_REGISTER", "WRITING_SPELLING",
                    "WRITING_PLURAL_FORM"}) {
                assertEquals("APPROVED", scalar(connection,
                        "SELECT review_status FROM error_types WHERE code = '" + code + "'"), code);
                assertEquals("true", scalar(connection,
                        "SELECT ('WRITING' = ANY(skills))::text FROM error_types WHERE code = '" + code + "'"), code);
            }
            assertEquals("{TASK_1}", scalar(connection,
                    "SELECT writing_tasks::text FROM error_types WHERE code = 'DATA_LANGUAGE'"));
            assertEquals("{}", scalar(connection,
                    "SELECT writing_tasks::text FROM error_types WHERE code = 'MISSING_VERB'"));
        }
    }

    @Test
    void taskSpecificCodesAreFilteredToTheirTask() throws Exception {
        try (Connection connection = connection()) {
            assertEquals("{TASK_1}", scalar(connection,
                    "SELECT writing_tasks::text FROM error_types WHERE code = 'T1_DATA_MISREPORTED'"));
            assertEquals("{TASK_2}", scalar(connection,
                    "SELECT writing_tasks::text FROM error_types WHERE code = 'OFF_TOPIC'"));
            assertEquals("{TASK_2}", scalar(connection,
                    "SELECT writing_tasks::text FROM error_types WHERE code = 'NO_CLEAR_POSITION'"));
            assertEquals("TASK", scalar(connection,
                    "SELECT category FROM error_types WHERE code = 'NO_CLEAR_POSITION'"));
        }
    }

    @Test
    void offTopicPointsAtTheOpinionPoint() throws Exception {
        try (Connection connection = connection()) {
            assertEquals("TR_W_OPINION", scalar(connection, """
                    SELECT kp.code FROM error_types et JOIN knowledge_points kp ON kp.id = et.knowledge_point_id
                    WHERE et.code = 'OFF_TOPIC'
                    """));
        }
    }

    private static Connection connection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static String scalar(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            assertTrue(rows.next(), "no row for: " + sql);
            return rows.getString(1);
        }
    }
}
