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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** After every migration, the grammar error codes an examiner can mark name their grammar point and its error trap. */
@Testcontainers(disabledWithoutDocker = true)
class GrammarErrorCodeMappingMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    @Test
    void eachApprovedCodeNamesItsGrammarPointAndHasATrapPointUnderIt() throws Exception {
        Map<String, String> expectedParent = Map.of(
                "ARTICLE", "G55_ARTICLES",
                "SUBJECT_VERB_AGREEMENT", "G55_SUBJECT_VERB_AGREEMENT",
                "PLURAL_FORM", "G55_ARTICLES",
                "TENSE", "G60_PERFECT_TENSES",
                "SENTENCE_FRAGMENT", "G55_COMPLEX_SENTENCES");
        try (Connection connection = connection()) {
            for (var entry : expectedParent.entrySet()) {
                String code = entry.getKey();
                assertEquals(entry.getValue(), scalar(connection, """
                        SELECT kp.code FROM error_types et JOIN knowledge_points kp ON kp.id = et.knowledge_point_id
                        WHERE et.code = '%s'
                        """.formatted(code)), code + " names its grammar point");
                assertEquals("TRAP_" + code, scalar(connection, """
                        SELECT trap.code FROM error_types et JOIN knowledge_points trap ON trap.id = et.trap_knowledge_point_id
                        WHERE et.code = '%s'
                        """.formatted(code)), code + " has its trap point");
                assertEquals("ACTIVE", scalar(connection, """
                        SELECT trap.status FROM error_types et JOIN knowledge_points trap ON trap.id = et.trap_knowledge_point_id
                        WHERE et.code = '%s'
                        """.formatted(code)), code + " trap point is active");
                assertEquals("true", scalar(connection, """
                        SELECT (trap.topic_id = parent.topic_id)::text FROM error_types et
                        JOIN knowledge_points trap ON trap.id = et.trap_knowledge_point_id
                        JOIN knowledge_points parent ON parent.id = et.knowledge_point_id
                        WHERE et.code = '%s'
                        """.formatted(code)), code + " trap point sits under the same topic");
            }
        }
    }

    @Test
    void codesWithoutAnApprovedPointStayUnmappedAndExistingMappingsAreUnchanged() throws Exception {
        try (Connection connection = connection()) {
            // OFF_TOPIC has no approved point. NO_CLEAR_POSITION got one later (V125) but still has no trap point,
            // because no practice sets with trap questions exist for it yet.
            assertNull(scalar(connection, "SELECT knowledge_point_id::text FROM error_types WHERE code = 'OFF_TOPIC'"),
                    "OFF_TOPIC");
            assertNull(scalar(connection, "SELECT trap_knowledge_point_id::text FROM error_types WHERE code = 'OFF_TOPIC'"),
                    "OFF_TOPIC");
            assertEquals("TR_W_OPINION", scalar(connection, """
                    SELECT kp.code FROM error_types et JOIN knowledge_points kp ON kp.id = et.knowledge_point_id
                    WHERE et.code = 'NO_CLEAR_POSITION'
                    """), "NO_CLEAR_POSITION point");
            assertNull(scalar(connection, "SELECT trap_knowledge_point_id::text FROM error_types WHERE code = 'NO_CLEAR_POSITION'"),
                    "NO_CLEAR_POSITION");
            assertEquals("DEMO_WRITING_LINKING", scalar(connection, """
                    SELECT kp.code FROM error_types et JOIN knowledge_points kp ON kp.id = et.knowledge_point_id
                    WHERE et.code = 'LINKER_MISUSE'
                    """), "LINKER_MISUSE keeps its point");
        }
    }

    @Test
    void theMigrationRefusesAParentPointThatIsNotActive() throws Exception {
        // The migration's own guard: every mapped point is active under an active topic and course, so the learner tree can show it.
        try (Connection connection = connection()) {
            assertTrue("0".equals(scalar(connection, """
                    SELECT count(*)::text FROM error_types et
                    JOIN knowledge_points parent ON parent.id = et.knowledge_point_id
                    JOIN topics t ON t.id = parent.topic_id
                    JOIN courses c ON c.id = t.course_id
                    WHERE et.code IN ('ARTICLE','SUBJECT_VERB_AGREEMENT','PLURAL_FORM','TENSE','SENTENCE_FRAGMENT')
                      AND (parent.status <> 'ACTIVE' OR t.status <> 'ACTIVE' OR c.status <> 'ACTIVE')
                    """)), "mapped parent points must be reachable");
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
