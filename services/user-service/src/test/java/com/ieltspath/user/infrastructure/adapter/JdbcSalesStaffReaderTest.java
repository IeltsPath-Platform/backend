package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.port.SalesStaffReader.SalesAccount;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class JdbcSalesStaffReaderTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;
    static JdbcSalesStaffReader reader;

    @BeforeAll
    static void setup() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        reader = new JdbcSalesStaffReader(new NamedParameterJdbcTemplate(dataSource));
    }

    @Test
    void salesAccountsAreListedWithTheirActiveFlagAndOtherRolesLeftOut() {
        UUID active = user("ACTIVE", "SALES_STAFF");
        UUID locked = user("LOCKED", "SALES_STAFF");
        UUID examiner = user("ACTIVE", "EXAMINER");

        List<SalesAccount> accounts = reader.findAll();

        assertTrue(accounts.stream().anyMatch(a -> a.id().equals(active) && a.active()));
        assertTrue(accounts.stream().anyMatch(a -> a.id().equals(locked) && !a.active()));
        assertFalse(accounts.stream().anyMatch(a -> a.id().equals(examiner)));
    }

    @Test
    void theLearnerProfileNameTakesPrecedenceOverTheAccountName() {
        UUID id = user("ACTIVE", "SALES_STAFF");
        jdbc.update("INSERT INTO learner_profiles(user_id, display_name) VALUES (?, ?)", id, "Tư vấn Lan");

        SalesAccount found = reader.findAll().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();

        assertEquals("Tư vấn Lan", found.displayName());
    }

    private static UUID user(String status, String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,email,full_name,password_hash,status) VALUES (?,?,'Account name','test-only',?)",
                id, id + "@example.test", status);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) SELECT ?, id FROM roles WHERE name=?", id, role);
        return id;
    }
}
