package com.ieltspath.user.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
class RoleSetMigrationTest {
    private static final List<String> ROLES =
            List.of("ADMIN", "CONTENT_AUTHOR", "CUSTOMER", "EXAMINER", "SALES_STAFF", "STAFF_MANAGER");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static Flyway flyway() {
        return Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").cleanDisabled(false).load();
    }

    @Test
    void endsWithExactlyTheRolesTheSystemKnows() throws Exception {
        Flyway flyway = flyway();
        flyway.clean();
        flyway.migrate();

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            assertEquals(ROLES, roleNames(statement));
            assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO roles (name) VALUES ('STAFF')"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO roles (name) VALUES ('MENTOR')"));
        }
    }

    @Test
    void movesAnAccountThatHeldStaffToSalesStaff() throws Exception {
        Flyway flyway = flyway();
        flyway.clean();
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target("10").load().migrate();
        UUID holder = UUID.randomUUID();
        UUID holderOfBoth = UUID.randomUUID();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            for (UUID id : List.of(holder, holderOfBoth)) {
                statement.executeUpdate("INSERT INTO users (id, email, full_name, password_hash, status) VALUES ('"
                        + id + "', '" + id + "@example.com', 'Test User', 'hash', 'ACTIVE')");
            }
            grant(statement, holder, "STAFF");
            grant(statement, holderOfBoth, "STAFF");
            grant(statement, holderOfBoth, "SALES_STAFF");
        }

        flyway.migrate();

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            assertEquals(List.of("SALES_STAFF"), rolesOf(statement, holder));
            assertEquals(List.of("SALES_STAFF"), rolesOf(statement, holderOfBoth));
            assertEquals(ROLES, roleNames(statement));
        }
    }

    private static void grant(Statement statement, UUID userId, String role) throws SQLException {
        statement.executeUpdate("INSERT INTO user_roles (user_id, role_id) SELECT '" + userId
                + "', id FROM roles WHERE name = '" + role + "'");
    }

    private static List<String> roleNames(Statement statement) throws SQLException {
        List<String> names = new ArrayList<>();
        try (var rows = statement.executeQuery("SELECT name FROM roles ORDER BY name")) {
            while (rows.next()) {
                names.add(rows.getString(1));
            }
        }
        return names;
    }

    private static List<String> rolesOf(Statement statement, UUID userId) throws SQLException {
        List<String> names = new ArrayList<>();
        try (var rows = statement.executeQuery("SELECT r.name FROM user_roles ur JOIN roles r ON r.id = ur.role_id "
                + "WHERE ur.user_id = '" + userId + "' ORDER BY r.name")) {
            while (rows.next()) {
                names.add(rows.getString(1));
            }
        }
        return names;
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
