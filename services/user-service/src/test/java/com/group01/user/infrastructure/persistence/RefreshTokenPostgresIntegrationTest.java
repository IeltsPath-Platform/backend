package com.group01.user.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class RefreshTokenPostgresIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private Flyway flyway;

    @BeforeEach
    void migrateDatabase() {
        flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void flywayAppliesAllUserServiceMigrationsAndOnlyOneConcurrentRefreshCanConsumeAToken() throws Exception {
        UUID userId = UUID.randomUUID();
        String tokenHash = "hash-" + UUID.randomUUID();
        insertUser(userId);
        insertRefreshToken(userId, tokenHash);

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Integer> affectedRows = executor.invokeAll(List.of(
                            consumeToken(barrier, tokenHash),
                            consumeToken(barrier, tokenHash)))
                    .stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertThat(affectedRows).containsExactlyInAnyOrder(1, 0);
            assertThat(revokedTokenCount(tokenHash)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Integer> consumeToken(CyclicBarrier barrier, String tokenHash) {
        return () -> {
            barrier.await();
            try (Connection connection = POSTGRES.createConnection("");
                 PreparedStatement statement = connection.prepareStatement("""
                         update refresh_tokens
                            set revoked_at = current_timestamp
                          where token_hash = ?
                            and revoked_at is null
                            and expires_at > current_timestamp
                         """)) {
                statement.setString(1, tokenHash);
                return statement.executeUpdate();
            }
        };
    }

    private void insertUser(UUID userId) throws Exception {
        try (Connection connection = POSTGRES.createConnection("");
             PreparedStatement userStmt = connection.prepareStatement("""
                     insert into users (id, email, password_hash, status)
                     values (?, ?, ?, 'ACTIVE')
                     """);
             PreparedStatement profileStmt = connection.prepareStatement("""
                     insert into user_profiles (user_id, full_name)
                     values (?, ?)
                     """)) {
            userStmt.setObject(1, userId);
            userStmt.setString(2, userId + "@example.com");
            userStmt.setString(3, "hash");
            userStmt.executeUpdate();

            profileStmt.setObject(1, userId);
            profileStmt.setString(2, "Integration User");
            profileStmt.executeUpdate();
        }
    }

    private void insertRefreshToken(UUID userId, String tokenHash) throws Exception {
        try (Connection connection = POSTGRES.createConnection("");
             PreparedStatement statement = connection.prepareStatement("""
                     insert into refresh_tokens (id, user_id, token_hash, expires_at)
                     values (?, ?, ?, ?)
                     """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, userId);
            statement.setString(3, tokenHash);
            statement.setObject(4, LocalDateTime.now().plusHours(1));
            statement.executeUpdate();
        }
    }

    private int revokedTokenCount(String tokenHash) throws Exception {
        try (Connection connection = POSTGRES.createConnection("");
             PreparedStatement statement = connection.prepareStatement("""
                     select count(*)
                     from refresh_tokens
                     where token_hash = ? and revoked_at is not null
                     """)) {
            statement.setString(1, tokenHash);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}
