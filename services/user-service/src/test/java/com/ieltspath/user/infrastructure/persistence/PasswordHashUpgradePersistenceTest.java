package com.ieltspath.user.infrastructure.persistence;

import com.ieltspath.user.infrastructure.persistence.repository.UserJpaRepository;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PasswordHashUpgradePersistenceTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired UserJpaRepository users;
    @Autowired JdbcTemplate jdbc;

    @Test
    void conditionalHashUpgradeCannotOverwriteAConcurrentPasswordChange() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,email,full_name,password_hash,status) VALUES(?,?,?,'old-hash','ACTIVE')",
                id, id + "@example.test", "Test User");
        jdbc.update("UPDATE users SET password_hash = 'concurrent-change' WHERE id = ?", id);

        assertThat(users.updatePasswordHashIfUnchanged(id, "old-hash", "upgraded-hash")).isZero();
        assertThat(jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, id))
                .isEqualTo("concurrent-change");
    }
}
