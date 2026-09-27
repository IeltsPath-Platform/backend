package com.group01.content.infrastructure.persistence;

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

@Testcontainers(disabledWithoutDocker = true)
class BandRangeMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @Test
    void seededContentKeepsAnOpenBand() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            var rows = statement.executeQuery(
                    "SELECT count(*) FROM topics WHERE code = 'DEMO_READING' AND band_min IS NULL AND band_max IS NULL");
            rows.next();
            assertEquals(1, rows.getInt(1));
        }
    }

    @Test
    void validBandRangesAreStored() throws Exception {
        UUID topicId = insertTopic("4.0", "5.5");
        execute("INSERT INTO knowledge_points (id, topic_id, code, name, kind, learning_type, band_min, band_max) "
                + "VALUES ('" + UUID.randomUUID() + "', '" + topicId + "', 'KP-" + UUID.randomUUID()
                + "', 'Kp', 'GRAMMAR', 'PROCEDURE', 6.0, NULL)");
    }

    @Test
    void invalidBandRangesAreRejected() {
        assertThrows(SQLException.class, () -> insertTopic("7.0", "5.0"));
        assertThrows(SQLException.class, () -> insertTopic("9.5", null));
        assertThrows(SQLException.class, () -> insertTopic("4.3", null));
    }

    private static UUID insertTopic(String min, String max) throws SQLException {
        UUID id = UUID.randomUUID();
        execute("INSERT INTO topics (id, code, name, band_min, band_max) VALUES ('" + id + "', 'T-" + id
                + "', 'Topic', " + min + ", " + max + ")");
        return id;
    }

    private static void execute(String sql) throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
