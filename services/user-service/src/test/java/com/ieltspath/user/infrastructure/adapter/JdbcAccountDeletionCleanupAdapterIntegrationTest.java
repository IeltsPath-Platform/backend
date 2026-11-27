package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.events.user.UserDeletedV1;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Mail events of a deleted account are dropped on real PostgreSQL; its deletion event and other users' mail stay. */
@Testcontainers(disabledWithoutDocker = true)
class JdbcAccountDeletionCleanupAdapterIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    void dropsTheDeletedUsersMailEventsButKeepsTheDeletionEvent() {
        UUID deleted = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        event(deleted, "user.password-reset-requested.v1");
        event(deleted, "user.email-verification-requested.v1");
        event(deleted, UserDeletedV1.EVENT_TYPE);
        event(other, "user.password-reset-requested.v1");

        new JdbcAccountDeletionCleanupAdapter(jdbc).anonymizeProfileAndDeleteIdentityData(deleted);

        assertThat(jdbc.queryForList("SELECT event_type FROM outbox_events WHERE aggregate_id = ?", String.class, deleted))
                .containsExactly(UserDeletedV1.EVENT_TYPE);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events WHERE aggregate_id = ?", Integer.class, other))
                .isEqualTo(1);
    }

    private static void event(UUID userId, String type) {
        jdbc.update("INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, payload) VALUES (?, 'User', ?, ?, ?)",
                UUID.randomUUID(), userId, type, "{\"email\": \"someone@example.com\"}");
    }
}
