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
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class KnowledgeGraphCompletionMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .target("73")
                .load()
                .migrate();
    }

    @Test
    void everyKnowledgePointHasCefrLevelAndCanDo() throws Exception {
        assertThat(count("""
                SELECT count(*) FROM knowledge_points
                WHERE cefr_level IS NULL OR can_do IS NULL OR btrim(can_do) = ''
                """)).isZero();
    }

    @Test
    void everyActiveKnowledgePointHasAnEdgeToAnotherActivePoint() throws Exception {
        assertThat(count("""
                SELECT count(*) FROM knowledge_points k
                WHERE k.status = 'ACTIVE'
                  AND NOT EXISTS (
                      SELECT 1 FROM knowledge_point_prerequisites p
                      JOIN knowledge_points other
                        ON other.id = CASE WHEN p.knowledge_point_id = k.id THEN p.prerequisite_id
                                           ELSE p.knowledge_point_id END
                      WHERE (p.knowledge_point_id = k.id OR p.prerequisite_id = k.id) AND other.status = 'ACTIVE')
                """)).isZero();
    }

    @Test
    void noPrerequisiteHasAHigherCefrLevelThanItsDependent() throws Exception {
        assertThat(count("""
                SELECT count(*) FROM knowledge_point_prerequisites p
                JOIN knowledge_points target ON target.id = p.knowledge_point_id
                JOIN knowledge_points prerequisite ON prerequisite.id = p.prerequisite_id
                WHERE array_position(ARRAY['A1','A2','B1','B2','C1','C2'], prerequisite.cefr_level)
                    > array_position(ARRAY['A1','A2','B1','B2','C1','C2'], target.cefr_level)
                """)).isZero();
    }

    @Test
    void speakingAndEntryLevelDraftsStayInactiveInTopicsWithoutACourse() throws Exception {
        assertThat(count("SELECT count(*) FROM knowledge_points WHERE skill = 'SPEAKING'")).isEqualTo(8);
        assertThat(count("""
                SELECT count(*) FROM knowledge_points
                WHERE id::text LIKE '73000000-0000-4000-8000-02%' AND status <> 'INACTIVE'
                """)).isZero();
        assertThat(count("""
                SELECT count(*) FROM knowledge_points k
                JOIN topics t ON t.id = k.topic_id
                WHERE k.id::text LIKE '73000000-0000-4000-8000-02%'
                  AND (t.course_id IS NOT NULL OR t.status <> 'INACTIVE')
                """)).isZero();
        assertThat(count("SELECT count(*) FROM knowledge_points WHERE cefr_level IN ('A1', 'A2')")).isEqualTo(5);
    }

    @Test
    void readingLessonKnowledgePointsFormTheLessonOrderChain() throws Exception {
        assertThat(count("""
                SELECT count(*) FROM knowledge_point_prerequisites p
                JOIN knowledge_points target ON target.id = p.knowledge_point_id
                JOIN knowledge_points prerequisite ON prerequisite.id = p.prerequisite_id
                WHERE (prerequisite.code, target.code) IN (
                    ('DR_TOPIC_SENTENCE', 'DEMO_READING_MAIN_IDEA'),
                    ('DEMO_READING_MAIN_IDEA', 'DR_IDEA_OR_DETAIL'),
                    ('DR_IDEA_OR_DETAIL', 'DR_MATCHING_HEADINGS'))
                """)).isEqualTo(3);
    }

    private static long count(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                     POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }
}
