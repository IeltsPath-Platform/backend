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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** After every migration, the Writing error codes added for word class and the hints made clearer for the examiner draft. */
@Testcontainers(disabledWithoutDocker = true)
class WritingWordFormMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    @Test
    void wordFormIsAnApprovedWritingCodeOnTheWordFormsPointWithNoTrapPointYet() throws Exception {
        try (Connection connection = connection()) {
            assertEquals("ACTIVE", scalar(connection, "SELECT status FROM error_types WHERE code = 'WORD_FORM'"));
            assertEquals("APPROVED", scalar(connection, "SELECT review_status FROM error_types WHERE code = 'WORD_FORM'"));
            assertEquals("GRAMMAR", scalar(connection, "SELECT category FROM error_types WHERE code = 'WORD_FORM'"));
            assertEquals("false", scalar(connection, "SELECT fallback::text FROM error_types WHERE code = 'WORD_FORM'"));
            assertEquals("true", scalar(connection, "SELECT ('WRITING' = ANY (skills))::text FROM error_types WHERE code = 'WORD_FORM'"));
            assertEquals("V55_WORD_FORMS", scalar(connection, """
                    SELECT kp.code FROM error_types et JOIN knowledge_points kp ON kp.id = et.knowledge_point_id
                    WHERE et.code = 'WORD_FORM'
                    """));
            assertNull(scalar(connection, "SELECT trap_knowledge_point_id::text FROM error_types WHERE code = 'WORD_FORM'"),
                    "no trap point until its practice sets are written");
        }
    }

    @Test
    void theWordFormPointIsReachableByALearner() throws Exception {
        try (Connection connection = connection()) {
            assertEquals("0", scalar(connection, """
                    SELECT count(*)::text FROM error_types et
                    JOIN knowledge_points parent ON parent.id = et.knowledge_point_id
                    JOIN topics t ON t.id = parent.topic_id
                    JOIN courses c ON c.id = t.course_id
                    WHERE et.code = 'WORD_FORM'
                      AND (parent.status <> 'ACTIVE' OR t.status <> 'ACTIVE' OR c.status <> 'ACTIVE')
                    """));
        }
    }

    @Test
    void theClearerHintsNameWhatTheExaminerAskedFor() throws Exception {
        try (Connection connection = connection()) {
            assertTrue(scalar(connection, "SELECT fix_hint FROM error_types WHERE code = 'T1_DATA_MISREPORTED'").contains("bản đồ"));
            assertTrue(scalar(connection, "SELECT fix_hint FROM error_types WHERE code = 'T1_DATA_MISREPORTED'").contains("quy trình"));
            assertTrue(scalar(connection, "SELECT fix_hint FROM error_types WHERE code = 'VERB_FORM'").contains("growed"));
            assertEquals("Ví dụ không liên quan", scalar(connection, "SELECT name FROM error_types WHERE code = 'T2_IRRELEVANT_EXAMPLE'"));
            assertTrue(scalar(connection, "SELECT fix_hint FROM error_types WHERE code = 'UNDERDEVELOPED_IDEA'").contains("quá chung"));
        }
    }

    @Test
    void codesStillUndecidedAreLeftAlone() throws Exception {
        // Dropping T2_NO_CONCLUSION and splitting the NO_CLEAR_POSITION cap wait for the product owner.
        try (Connection connection = connection()) {
            assertEquals("ACTIVE", scalar(connection, "SELECT status FROM error_types WHERE code = 'T2_NO_CONCLUSION'"));
            assertNotNull(scalar(connection, "SELECT code FROM error_types WHERE code = 'NO_CLEAR_POSITION'"));
        }
    }

    private static Connection connection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static String scalar(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            return rows.next() ? rows.getString(1) : null;
        }
    }
}
