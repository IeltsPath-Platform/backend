package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A lesson document is either a link or an uploaded file, and only a document can be a file. */
@Testcontainers(disabledWithoutDocker = true)
class LessonResourceFileMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static final String KEY = "lesson-document/0b1f7c9e-3a40-4d3a-9a52-1d2c8e0a7f11/9c2e5d3a-77b1-4f0a-8f6e-2b8d1c4a9e10.pdf";

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target("114").load().migrate();
    }

    @Test
    void aDocumentCanBeAFileWithoutALinkAndLinksStillWork() throws Exception {
        try (var connection = connect(); var sql = connection.createStatement()) {
            UUID lessonId = insertLesson(sql, "FILE_OK");
            sql.executeUpdate(row(lessonId, "DOCUMENT", "'Giáo trình'", "NULL", "'" + KEY + "'", 0));
            sql.executeUpdate(row(lessonId, "DOCUMENT", "'Liên kết'", "'https://drive.google.com/x'", "NULL", 1));
            sql.executeUpdate(row(lessonId, "SLIDE", "'Slide'", "'https://docs.google.com/presentation/d/x'", "NULL", 0));
            try (var rows = sql.executeQuery("SELECT count(*) FROM lesson_resources WHERE lesson_id='" + lessonId
                    + "' AND file_key IS NOT NULL AND url IS NULL")) {
                assertTrue(rows.next());
                assertEquals(1, rows.getInt(1));
            }
        }
    }

    @Test
    void aRowNeedsExactlyOneOfLinkAndFileAndOnlyADocumentMayBeAFile() throws Exception {
        try (var connection = connect(); var sql = connection.createStatement()) {
            UUID lessonId = insertLesson(sql, "FILE_BAD");
            for (String bad : new String[]{
                    row(lessonId, "DOCUMENT", "'Cả hai'", "'https://a.com'", "'" + KEY + "'", 0),
                    row(lessonId, "DOCUMENT", "'Không có gì'", "NULL", "NULL", 1),
                    row(lessonId, "SLIDE", "'Slide là file'", "NULL", "'" + KEY + "'", 0),
                    row(lessonId, "DOCUMENT", "'Khóa lạ'", "NULL", "'content-image/x/y.png'", 2),
                    row(lessonId, "DOCUMENT", "'Link hỏng'", "'ftp://a.com'", "NULL", 3)}) {
                assertEquals("23514", assertThrows(SQLException.class, () -> sql.executeUpdate(bad)).getSQLState(), bad);
            }
        }
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static UUID insertLesson(Statement sql, String code) throws SQLException {
        UUID topicId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        sql.executeUpdate("INSERT INTO topics (id,code,name) VALUES ('" + topicId + "','RES_" + code + "','Resources')");
        sql.executeUpdate("INSERT INTO lessons (id,topic_id,code,title,sort_order) VALUES ('" + lessonId + "','"
                + topicId + "','RES_" + code + "','Files',1)");
        return lessonId;
    }

    private static String row(UUID lessonId, String type, String title, String url, String fileKey, int sortOrder) {
        return "INSERT INTO lesson_resources (lesson_id,resource_type,title,url,file_key,sort_order) VALUES ('"
                + lessonId + "','" + type + "'," + title + "," + url + "," + fileKey + "," + sortOrder + ")";
    }
}
