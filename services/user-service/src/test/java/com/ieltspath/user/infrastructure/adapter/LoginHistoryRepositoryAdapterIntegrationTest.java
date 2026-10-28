package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.domain.aggregate.LoginAttempt;
import com.ieltspath.user.domain.vo.LoginFailureReason;
import com.ieltspath.user.domain.vo.OwnedPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LoginHistoryRepositoryAdapter.class)
class LoginHistoryRepositoryAdapterIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    LoginHistoryRepositoryAdapter repository;
    @Autowired
    JdbcTemplate jdbc;

    private UUID newUser() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email, full_name, password_hash, status) VALUES (?, ?, 'Test User', 'hash', 'ACTIVE')",
                id, id + "@example.com");
        return id;
    }

    private void record(UUID userId, LocalDateTime at, boolean success) {
        repository.save(LoginAttempt.builder()
                .userId(userId)
                .success(success)
                .failureReason(success ? null : LoginFailureReason.BAD_PASSWORD)
                .ipAddress("10.0.0.1")
                .userAgent("test")
                .createdAt(at)
                .build());
    }

    @Test
    void listsOneUsersAttemptsNewestFirstWithTheTotal() {
        UUID userId = newUser();
        UUID otherUser = newUser();
        LocalDateTime now = LocalDateTime.of(2026, 10, 29, 12, 0);
        record(userId, now.minusHours(3), true);
        record(userId, now.minusHours(1), false);
        record(userId, now.minusHours(2), true);
        record(otherUser, now, true);

        OwnedPage<LoginAttempt> firstPage = repository.findByUserId(userId, 0, 2);
        OwnedPage<LoginAttempt> secondPage = repository.findByUserId(userId, 1, 2);

        assertThat(firstPage.total()).isEqualTo(3);
        assertThat(firstPage.items()).extracting(LoginAttempt::getCreatedAt)
                .containsExactly(now.minusHours(1), now.minusHours(2));
        assertThat(firstPage.items().get(0).getFailureReason()).isEqualTo(LoginFailureReason.BAD_PASSWORD);
        assertThat(secondPage.items()).extracting(LoginAttempt::getCreatedAt).containsExactly(now.minusHours(3));
    }

    @Test
    void deletesOnlyAttemptsOlderThanTheCutoff() {
        UUID userId = newUser();
        LocalDateTime cutoff = LocalDateTime.of(2026, 7, 31, 3, 30);
        record(userId, cutoff.minusDays(10), true);
        record(userId, cutoff.minusSeconds(1), false);
        record(userId, cutoff, true);
        record(userId, cutoff.plusDays(5), true);

        int removed = repository.deleteOlderThan(cutoff);

        assertThat(removed).isEqualTo(2);
        assertThat(repository.findByUserId(userId, 0, 10).items()).extracting(LoginAttempt::getCreatedAt)
                .containsExactly(cutoff.plusDays(5), cutoff);
    }
}
