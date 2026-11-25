package com.ieltspath.user.infrastructure.persistence.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class UserJpaRepositoryPaginationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired UserJpaRepository repository;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clearUsers() {
        jdbc.execute("TRUNCATE users CASCADE");
    }

    @Test
    void consecutivePagesDoNotRepeatRowsAndCountUsesTheSameFilters() {
        UUID first = insertUser("ada@example.test", "Sample Ada", "ACTIVE", "ADMIN", 1);
        UUID second = insertUser("bea@example.test", "Sample Bea", "ACTIVE", "ADMIN", 2);
        insertUser("cee@example.test", "Sample Cee", "ACTIVE", "CUSTOMER", 3);
        insertUser("other@example.test", "Other User", "LOCKED", "ADMIN", 4);

        var firstPage = repository.findPageIds("ADMIN", "ACTIVE", "sample", org.springframework.data.domain.PageRequest.of(0, 1));
        var secondPage = repository.findPageIds("ADMIN", "ACTIVE", "sample", org.springframework.data.domain.PageRequest.of(1, 1));

        assertThat(firstPage.getContent()).containsExactly(first);
        assertThat(secondPage.getContent()).containsExactly(second);
        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(secondPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    private UUID insertUser(String email, String fullName, String status, String role, int minute) {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, minute);
        jdbc.update("INSERT INTO users (id,email,full_name,password_hash,status,created_at,updated_at) "
                        + "VALUES (?,?,?,'hash',?,?,?)", id, email, fullName, status, createdAt, createdAt);
        jdbc.update("INSERT INTO user_roles (user_id,role_id) SELECT ?, id FROM roles WHERE name = ?", id, role);
        return id;
    }
}
