package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
class SectionOrderMigrationTest {
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
    void swappingTwoSectionOrdersSucceedsWhenTheCheckIsDeferred() throws Exception {
        UUID version = newDraftVersion();
        UUID first = newSection(version, 1);
        UUID second = newSection(version, 2);

        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET CONSTRAINTS uq_content_sections_sort_order DEFERRED");
            }
            setSortOrder(connection, first, 2);
            setSortOrder(connection, second, 1);
            connection.commit();
        }

        assertEquals(2, sortOrder(first));
        assertEquals(1, sortOrder(second));
    }

    @Test
    void duplicateSortOrderIsStillRefusedImmediatelyByDefault() throws Exception {
        UUID version = newDraftVersion();
        UUID first = newSection(version, 1);
        newSection(version, 2);

        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            assertThrows(SQLException.class, () -> setSortOrder(connection, first, 2));
        }
    }

    private static UUID newDraftVersion() throws SQLException {
        UUID packageId = UUID.randomUUID();
        try (Connection connection = connection()) {
            execute(connection, "INSERT INTO content_packages (id, code, title, package_type) VALUES (?, ?, ?, 'PRACTICE_SET')",
                    packageId, "ORDER-" + packageId, "Order test");
            UUID version = UUID.randomUUID();
            execute(connection, "INSERT INTO content_package_versions (id, package_id, version_number, status) VALUES (?, ?, 1, 'DRAFT')",
                    version, packageId);
            return version;
        }
    }

    private static UUID newSection(UUID version, int sortOrder) throws SQLException {
        UUID section = UUID.randomUUID();
        try (Connection connection = connection()) {
            execute(connection, "INSERT INTO content_sections (id, package_version_id, title, sort_order) VALUES (?, ?, ?, ?)",
                    section, version, "Section " + sortOrder, sortOrder);
        }
        return section;
    }

    private static void setSortOrder(Connection connection, UUID section, int sortOrder) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE content_sections SET sort_order = ? WHERE id = ?")) {
            statement.setInt(1, sortOrder);
            statement.setObject(2, section);
            statement.executeUpdate();
        }
    }

    private static int sortOrder(UUID section) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("SELECT sort_order FROM content_sections WHERE id = ?")) {
            statement.setObject(1, section);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }

    private static void execute(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                statement.setObject(i + 1, values[i]);
            }
            statement.executeUpdate();
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
