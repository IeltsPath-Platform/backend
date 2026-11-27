package com.ieltspath.notification.infrastructure;

import com.ieltspath.notification.infrastructure.persistence.JdbcDeletedUserStore;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A phone that received pushes for one account and is then signed in to another: the device row moves to the second
 * account while the first account's deliveries still point at it. Deleting the second account must still finish.
 */
@Testcontainers(disabledWithoutDocker = true)
class UserDeletionSharedDeviceIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;
    private static DriverManagerDataSource dataSource;

    @BeforeAll
    static void migrate() {
        dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    void deletingTheAccountNowOnASharedPhoneRemovesItsDeviceAndNotifications() {
        UUID previous = UUID.randomUUID();
        UUID current = UUID.randomUUID();
        UUID device = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO notification_devices (id, user_id, installation_id, provider, platform, token, token_hash,
                    created_at, updated_at, last_registered_at)
                VALUES (?, ?, ?, 'FCM', 'ANDROID', 'token', ?, now(), now(), now())
                """, device, previous, UUID.randomUUID(), "a".repeat(64));
        UUID previousNotice = notification(previous);
        delivery(previousNotice, previous, device);
        // The same installation signs in as another account: the row is re-registered to it.
        jdbc.update("UPDATE notification_devices SET user_id = ?, registration_version = 2 WHERE id = ?", current, device);
        UUID currentNotice = notification(current);
        delivery(currentNotice, current, device);

        var store = new JdbcDeletedUserStore(jdbc);
        UUID eventId = UUID.randomUUID();
        store.recordDeletion(eventId, current, Instant.now());
        new UserDeletionJobWorker(store, new DataSourceTransactionManager(dataSource), 300, 10).processPending();

        assertThat(jdbc.queryForObject("SELECT status FROM user_deletion_jobs WHERE event_id = ?", String.class, eventId))
                .isEqualTo("DONE");
        assertThat(count("SELECT count(*) FROM notification_devices WHERE id = ?", device)).isZero();
        assertThat(count("SELECT count(*) FROM notifications WHERE recipient_id = ?", current)).isZero();
        assertThat(count("SELECT count(*) FROM notification_push_deliveries WHERE device_id = ?", device)).isZero();
        assertThat(count("SELECT count(*) FROM notifications WHERE id = ?", previousNotice))
                .as("the previous account keeps its inbox").isEqualTo(1);
    }

    private static UUID notification(UUID recipient) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO notifications (id, source_event_id, recipient_id, actor_id, event_type, created_at)
                VALUES (?, ?, ?, ?, 'COMMENT_CREATED', now())
                """, id, UUID.randomUUID(), recipient, UUID.randomUUID());
        return id;
    }

    private static void delivery(UUID notification, UUID recipient, UUID device) {
        jdbc.update("""
                INSERT INTO notification_push_deliveries (id, notification_id, recipient_id, device_id,
                    registration_version, status, next_attempt_at, expires_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, 1, 'SENT', now(), now() + interval '1 day', now(), now())
                """, UUID.randomUUID(), notification, recipient, device);
    }

    private static int count(String sql, UUID id) {
        Integer value = jdbc.queryForObject(sql, Integer.class, id);
        return value == null ? 0 : value;
    }
}
