package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.infrastructure.persistence.adapter.LessonResourceRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.mapper.LessonResourcePersistenceMapper;
import com.ieltspath.content.infrastructure.persistence.repository.LessonResourceJpaRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@Testcontainers(disabledWithoutDocker = true)
class LessonResourceMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target("28").load().migrate();
    }

    @Test
    void lessonSummaryIsOptionalMarkdown() throws Exception {
        try (var connection = connect(); var sql = connection.createStatement()) {
            UUID lessonId = insertLesson(sql, "SUMMARY");
            try (var rows = sql.executeQuery("SELECT summary_markdown FROM lessons WHERE id='" + lessonId + "'")) {
                assertTrue(rows.next());
                assertNull(rows.getString(1));
            }
            assertEquals(1, sql.executeUpdate(
                    "UPDATE lessons SET summary_markdown='## Learning Outcomes' WHERE id='" + lessonId + "'"));
        }
    }

    @Test
    void resourcesAreOrderedPerLessonAndType() throws Exception {
        try (var connection = connect(); var sql = connection.createStatement()) {
            UUID lessonId = insertLesson(sql, "ORDER");
            sql.executeUpdate(resource(lessonId, "SLIDE", "Slide", "https://docs.google.com/presentation/d/1", 0));
            sql.executeUpdate(resource(lessonId, "DOCUMENT", "Giáo trình", "https://drive.google.com/file/d/1", 0));
            sql.executeUpdate(resource(lessonId, "DOCUMENT", "Bài tập", "http://cdn.example.com/b.pdf", 1));

            assertEquals("23505", assertThrows(SQLException.class, () -> sql.executeUpdate(
                    resource(lessonId, "DOCUMENT", "Trùng thứ tự", "https://drive.google.com/file/d/2", 1))).getSQLState());
            try (var rows = sql.executeQuery("SELECT created_at IS NOT NULL AND updated_at IS NOT NULL FROM lesson_resources"
                    + " WHERE lesson_id='" + lessonId + "' AND resource_type='SLIDE'")) {
                assertTrue(rows.next());
                assertTrue(rows.getBoolean(1));
            }
        }
    }

    @Test
    void invalidResourcesAreRejected() throws Exception {
        try (var connection = connect(); var sql = connection.createStatement()) {
            UUID lessonId = insertLesson(sql, "INVALID");
            for (String row : new String[]{
                    resource(lessonId, "VIDEO", "Kiểu lạ", "https://example.com/a", 0),
                    resource(lessonId, "SLIDE", "   ", "https://example.com/a", 0),
                    resource(lessonId, "SLIDE", "Không phải link", "ftp://example.com/a", 0),
                    resource(lessonId, "SLIDE", "Không phải link", "javascript:alert(1)", 0),
                    resource(lessonId, "SLIDE", "Thứ tự âm", "https://example.com/a", -1)}) {
                assertEquals("23514", assertThrows(SQLException.class, () -> sql.executeUpdate(row)).getSQLState(), row);
            }
            assertEquals("23503", assertThrows(SQLException.class, () -> sql.executeUpdate(
                    resource(UUID.randomUUID(), "SLIDE", "Không có bài", "https://example.com/a", 0))).getSQLState());
        }
    }

    @Test
    void deletingALessonDeletesItsResources() throws Exception {
        try (var connection = connect(); var sql = connection.createStatement()) {
            UUID lessonId = insertLesson(sql, "CASCADE");
            sql.executeUpdate(resource(lessonId, "SLIDE", "Slide", "https://example.com/s", 0));
            sql.executeUpdate("DELETE FROM lessons WHERE id='" + lessonId + "'");
            try (var rows = sql.executeQuery("SELECT count(*) FROM lesson_resources WHERE lesson_id='" + lessonId + "'")) {
                assertTrue(rows.next());
                assertEquals(0, rows.getInt(1));
            }
        }
    }

    @Test
    void adapterReadsAndWritesTheLessonSummaryColumn() throws Exception {
        UUID lessonId;
        try (var connection = connect(); var sql = connection.createStatement()) {
            lessonId = insertLesson(sql, "ADAPTER");
        }
        var adapter = new LessonResourceRepositoryAdapter(mock(LessonResourceJpaRepository.class),
                Mappers.getMapper(LessonResourcePersistenceMapper.class), new NamedParameterJdbcTemplate(
                new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())));

        assertNull(adapter.findSummaryMarkdown(lessonId));
        adapter.updateSummaryMarkdown(lessonId, "## Learning Outcomes");
        assertEquals("## Learning Outcomes", adapter.findSummaryMarkdown(lessonId));
        adapter.updateSummaryMarkdown(lessonId, null);
        assertNull(adapter.findSummaryMarkdown(lessonId));
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static UUID insertLesson(Statement sql, String code) throws SQLException {
        UUID topicId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        sql.executeUpdate("INSERT INTO topics (id,code,name) VALUES ('" + topicId + "','RES_" + code + "','Resources')");
        sql.executeUpdate("INSERT INTO lessons (id,topic_id,code,title,sort_order) VALUES ('" + lessonId + "','"
                + topicId + "','RES_" + code + "','Nature & Environment',1)");
        return lessonId;
    }

    private static String resource(UUID lessonId, String type, String title, String url, int sortOrder) {
        return "INSERT INTO lesson_resources (lesson_id,resource_type,title,url,sort_order) VALUES ('" + lessonId + "','"
                + type + "','" + title + "','" + url + "'," + sortOrder + ")";
    }
}
