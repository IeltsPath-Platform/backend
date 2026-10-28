package com.ieltspath.user.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
class StaffRolesMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    @Test
    void seedsEveryRoleTheSystemKnows() throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT name FROM roles ORDER BY name")) {
            List<String> names = new ArrayList<>();
            while (rows.next()) {
                names.add(rows.getString(1));
            }
            assertEquals(List.of("ADMIN", "CONTENT_AUTHOR", "CUSTOMER", "EXAMINER", "SALES_STAFF", "STAFF", "STAFF_MANAGER"), names);
        }
    }

    @Test
    void stillRejectsARoleNobodyDefined() throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO roles (name) VALUES ('MENTOR')"));
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
