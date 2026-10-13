package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class LessonResourceSeedTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target("29").load().migrate();
    }

    @Test
    void demoLessonsArePublishedAndCarryASummaryOneSlideAndDocuments() throws Exception {
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var sql = connection.createStatement();
             var rows = sql.executeQuery("""
                     SELECT l.code, l.status, l.summary_markdown,
                            count(*) FILTER (WHERE r.resource_type = 'SLIDE') AS slides,
                            count(*) FILTER (WHERE r.resource_type = 'DOCUMENT') AS documents
                     FROM lessons l JOIN lesson_resources r ON r.lesson_id = l.id
                     GROUP BY l.code, l.status, l.summary_markdown ORDER BY l.code""")) {
            assertTrue(rows.next());
            assertDemoLesson(rows, "L1", 2);
            assertTrue(rows.next());
            assertDemoLesson(rows, "T1", 3);
            assertFalse(rows.next());
        }
    }

    private static void assertDemoLesson(java.sql.ResultSet rows, String code, int documents) throws Exception {
        assertEquals(code, rows.getString("code"));
        assertEquals("PUBLISHED", rows.getString("status"));
        assertTrue(rows.getString("summary_markdown").startsWith("## Learning Outcomes"));
        assertEquals(1, rows.getInt("slides"));
        assertEquals(documents, rows.getInt("documents"));
    }
}
