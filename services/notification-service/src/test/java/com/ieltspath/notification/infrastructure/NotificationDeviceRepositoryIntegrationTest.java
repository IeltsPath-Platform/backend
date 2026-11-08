package com.ieltspath.notification.infrastructure;

import com.ieltspath.notification.infrastructure.persistence.NotificationDeviceRepositoryAdapter;
import com.ieltspath.notification.infrastructure.persistence.NotificationRepositoryAdapter;
import com.ieltspath.notification.infrastructure.persistence.PushDeliveryRepositoryAdapter;
import com.ieltspath.notification.domain.vo.Notification;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
class NotificationDeviceRepositoryIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;
    static NotificationDeviceRepositoryAdapter devices;
    static NotificationRepositoryAdapter notifications;
    static PushDeliveryRepositoryAdapter deliveries;
    static TransactionTemplate transactions;

    @BeforeAll
    static void setup() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("filesystem:src/main/resources/db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        devices = new NotificationDeviceRepositoryAdapter(jdbc);
        notifications = new NotificationRepositoryAdapter(jdbc);
        deliveries = new PushDeliveryRepositoryAdapter(jdbc);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Test
    void registrationIsIdempotentAndInstallationOwnershipTransferInvalidatesOldVersion() {
        clean();
        UUID installation = UUID.randomUUID(), firstOwner = UUID.randomUUID(), nextOwner = UUID.randomUUID();
        register(firstOwner, installation, "token-one");
        register(firstOwner, installation, "token-one");
        assertEquals(1L, version(installation));

        register(nextOwner, installation, "token-two");
        assertEquals(2L, version(installation));
        transactions.executeWithoutResult(tx -> devices.disable(firstOwner, installation));
        assertEquals(1, activeMappings(installation));
        assertEquals(nextOwner, jdbc.queryForObject("SELECT user_id FROM notification_devices WHERE installation_id=?",
                UUID.class, installation));
    }

    @Test
    void oneActiveInstallationOwnsATokenEvenWhenRegistrationsRace() throws Exception {
        clean();
        UUID user = UUID.randomUUID();
        try (var pool = Executors.newFixedThreadPool(4)) {
            var tasks = java.util.stream.IntStream.range(0, 4).<Runnable>mapToObj(index -> () -> {
                UUID installation = UUID.randomUUID();
                register(user, installation, "shared-token");
            }).toList();
            var futures = tasks.stream().map(pool::submit).toList();
            for (var future : futures) future.get(30, TimeUnit.SECONDS);
        }
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM notification_devices WHERE active AND token_hash=?",
                Integer.class, sha256("shared-token")));
    }

    @Test
    void notificationEnqueuesOneDeliveryPerActiveDeviceAndRedeliveryAddsNone() {
        clean();
        UUID user = UUID.randomUUID();
        register(user, UUID.randomUUID(), "first-device-token");
        register(user, UUID.randomUUID(), "second-device-token");
        UUID event = UUID.randomUUID();
        Notification notification = new Notification(event, user, user, "access.subscription-granted.v1",
                null, null, null, java.time.Instant.now(), null);

        transactions.executeWithoutResult(tx -> {
            if (notifications.insertIfAbsent(event, notification)) deliveries.enqueueFor(notification);
        });
        transactions.executeWithoutResult(tx -> {
            if (notifications.insertIfAbsent(event, notification)) deliveries.enqueueFor(notification);
        });

        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM notifications WHERE id=?", Integer.class, event));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM notification_push_deliveries WHERE notification_id=?",
                Integer.class, event));
    }

    @Test
    void enqueueFailureRollsBackNotificationAndDeliveryTogether() {
        clean();
        UUID user = UUID.randomUUID();
        register(user, UUID.randomUUID(), "rollback-token");
        UUID event = UUID.randomUUID();
        Notification notification = new Notification(event, user, user, "access.subscription-granted.v1",
                null, null, null, java.time.Instant.now(), null);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                transactions.executeWithoutResult(tx -> {
                    notifications.insertIfAbsent(event, notification);
                    deliveries.enqueueFor(notification);
                    throw new IllegalStateException("forced rollback");
                }));

        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM notifications WHERE id=?", Integer.class, event));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM notification_push_deliveries WHERE notification_id=?",
                Integer.class, event));
    }

    @Test
    void claimsAreExclusiveAndAnExpiredLeaseGetsANewFencingId() {
        clean();
        UUID user = UUID.randomUUID();
        register(user, UUID.randomUUID(), "single-token");
        UUID event = UUID.randomUUID();
        Notification notification = new Notification(event, user, user, "community.user-followed.v1",
                null, null, null, java.time.Instant.now(), null);
        transactions.executeWithoutResult(tx -> {
            notifications.insertIfAbsent(event, notification);
            deliveries.enqueueFor(notification);
        });
        java.time.Instant now = java.time.Instant.now().plusSeconds(1);
        var firstClaim = transactions.execute(tx -> deliveries.claimDue(now, 2, now.plusSeconds(120), 8).getFirst());
        assertEquals(0, transactions.execute(tx -> deliveries.claimDue(now, 2, now.plusSeconds(120), 8).size()));

        java.time.Instant afterLease = now.plusSeconds(121);
        var recovered = transactions.execute(tx -> deliveries.claimDue(afterLease, 2, afterLease.plusSeconds(120), 8).getFirst());
        assertEquals(2, recovered.attemptCount());
        org.junit.jupiter.api.Assertions.assertNotEquals(firstClaim.claimId(), recovered.claimId());
        assertEquals(false, deliveries.markSent(firstClaim, afterLease, "stale-message"));
    }

    @Test
    void anOldRegistrationCannotBeMarkedSentAfterOwnershipMoves() {
        clean();
        UUID oldOwner = UUID.randomUUID();
        UUID installation = UUID.randomUUID();
        register(oldOwner, installation, "moving-token");
        UUID event = UUID.randomUUID();
        Notification notification = new Notification(event, oldOwner, oldOwner, "community.user-followed.v1",
                null, null, null, java.time.Instant.now(), null);
        transactions.executeWithoutResult(tx -> {
            notifications.insertIfAbsent(event, notification);
            deliveries.enqueueFor(notification);
        });
        java.time.Instant now = java.time.Instant.now().plusSeconds(1);
        var claim = transactions.execute(tx -> deliveries.claimDue(now, 2, now.plusSeconds(120), 8).getFirst());
        register(UUID.randomUUID(), installation, "moving-token");

        assertEquals(false, deliveries.markSent(claim, now.plusSeconds(2), "stale-message"));
        assertEquals("IN_FLIGHT", jdbc.queryForObject("SELECT status FROM notification_push_deliveries WHERE id=?",
                String.class, claim.id()));
    }

    private static void register(UUID user, UUID installation, String token) {
        transactions.executeWithoutResult(tx -> devices.register(user, installation, "FCM", "ANDROID", token, "1.0"));
    }

    private static void clean() {
        jdbc.update("DELETE FROM notification_push_deliveries");
        jdbc.update("DELETE FROM notifications");
        jdbc.update("DELETE FROM notification_devices");
    }

    private static long version(UUID installation) {
        return jdbc.queryForObject("SELECT registration_version FROM notification_devices WHERE installation_id=?",
                Long.class, installation);
    }

    private static int activeMappings(UUID installation) {
        return jdbc.queryForObject("SELECT count(*) FROM notification_devices WHERE installation_id=? AND active",
                Integer.class, installation);
    }

    private static String sha256(String token) throws Exception {
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
