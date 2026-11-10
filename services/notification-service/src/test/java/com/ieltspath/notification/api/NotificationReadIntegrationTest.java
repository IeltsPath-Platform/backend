package com.ieltspath.notification.api;

import com.ieltspath.commonsecurity.currentuser.CurrentUser;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.commonsecurity.role.CanonicalRoles;
import com.ieltspath.notification.api.exception.GlobalExceptionHandler;
import com.ieltspath.notification.application.NotificationInboxUseCase;
import com.ieltspath.notification.application.NotificationRetentionUseCase;
import com.ieltspath.notification.application.NotificationPreferenceService;
import com.ieltspath.notification.infrastructure.persistence.NotificationPreferenceRepositoryAdapter;
import com.ieltspath.notification.infrastructure.persistence.NotificationRepositoryAdapter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
class NotificationReadIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    static DriverManagerDataSource dataSource;
    static JdbcTemplate jdbc;

    @BeforeAll
    static void setupDatabase() {
        dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure()
                .dataSource(dataSource)
                .locations("filesystem:src/main/resources/db/migration")
                .load()
                .migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void cleanUp() {
        jdbc.execute("DELETE FROM notifications");
    }

    private MockMvc createMockMvc(UUID callerId) {
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        when(users.requireCurrentUser()).thenReturn(new CurrentUser(callerId, Set.of(CanonicalRoles.CUSTOMER)));
        NotificationPreferenceService prefs = new NotificationPreferenceService(new NotificationPreferenceRepositoryAdapter(jdbc));
        NotificationController controller = new NotificationController(
                new NotificationInboxUseCase(new NotificationRepositoryAdapter(jdbc)), users, prefs);
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void markReadReturnsNotFoundWhenNotificationDoesNotExist() throws Exception {
        UUID callerId = UUID.randomUUID();
        UUID nonExistentNotificationId = UUID.randomUUID();
        MockMvc mvc = createMockMvc(callerId);

        mvc.perform(put("/api/notifications/{id}/read", nonExistentNotificationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Notification not found"));
    }

    @Test
    void markReadReturnsNotFoundWhenNotificationBelongsToAnotherRecipient() throws Exception {
        UUID callerId = UUID.randomUUID();
        UUID otherRecipientId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        jdbc.update("INSERT INTO notifications(id, source_event_id, recipient_id, actor_id, event_type, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                notificationId, UUID.randomUUID(), otherRecipientId, actorId,
                "community.post-commented.v1", java.sql.Timestamp.from(Instant.now()));

        MockMvc mvc = createMockMvc(callerId);

        mvc.perform(put("/api/notifications/{id}/read", notificationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Notification not found"));
    }

    @Test
    void markReadSucceedsWhenNotificationBelongsToCaller() throws Exception {
        UUID callerId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        jdbc.update("INSERT INTO notifications(id, source_event_id, recipient_id, actor_id, event_type, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                notificationId, UUID.randomUUID(), callerId, actorId,
                "community.post-commented.v1", java.sql.Timestamp.from(Instant.now()));

        MockMvc mvc = createMockMvc(callerId);

        mvc.perform(put("/api/notifications/{id}/read", notificationId))
                .andExpect(status().isOk());

        java.sql.Timestamp readAt = jdbc.queryForObject(
                "SELECT read_at FROM notifications WHERE id = ?",
                java.sql.Timestamp.class,
                notificationId
        );
        assertNotNull(readAt);
    }

    @Test
    void deleteReturnsNotFoundForAnotherRecipientsNotification() throws Exception {
        UUID callerId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO notifications(id, source_event_id, recipient_id, actor_id, event_type, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                id, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "community.post-reacted.v1", java.sql.Timestamp.from(Instant.now()));

        createMockMvc(callerId).perform(delete("/api/notifications/{id}", id)).andExpect(status().isNotFound());
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM notifications WHERE id=?", Integer.class, id));
    }

    @Test
    void deleteReadRemovesOnlyTheCallersReadNotifications() throws Exception {
        UUID callerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Instant now = Instant.now();
        insert(callerId, now, now);
        insert(callerId, now, null);
        insert(otherId, now, now);

        createMockMvc(callerId).perform(delete("/api/notifications/read"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        org.junit.jupiter.api.Assertions.assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM notifications", Integer.class));
    }

    @Test
    void retentionDeletesOnlyAConfiguredBoundedBatchOfExpiredRows() {
        Instant now = Instant.now();
        Instant oldRead = now.minus(91, java.time.temporal.ChronoUnit.DAYS);
        Instant oldUnread = now.minus(181, java.time.temporal.ChronoUnit.DAYS);
        insert(UUID.randomUUID(), oldRead, oldRead);
        insert(UUID.randomUUID(), oldRead.minusSeconds(1), oldRead.minusSeconds(1));
        insert(UUID.randomUUID(), oldUnread, null);
        insert(UUID.randomUUID(), oldUnread.minusSeconds(1), null);
        insert(UUID.randomUUID(), now.minus(30, java.time.temporal.ChronoUnit.DAYS), now);
        insert(UUID.randomUUID(), now.minus(30, java.time.temporal.ChronoUnit.DAYS), null);

        NotificationRetentionUseCase retention = new NotificationRetentionUseCase(new NotificationRepositoryAdapter(jdbc));
        org.junit.jupiter.api.Assertions.assertEquals(2, retention.deleteExpiredBatch(now, 2));
        org.junit.jupiter.api.Assertions.assertEquals(4, jdbc.queryForObject("SELECT count(*) FROM notifications", Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(2, retention.deleteExpiredBatch(now, 2));
        org.junit.jupiter.api.Assertions.assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM notifications", Integer.class));
    }

    private void insert(UUID recipientId, Instant createdAt, Instant readAt) {
        jdbc.update("INSERT INTO notifications(id, source_event_id, recipient_id, actor_id, event_type, created_at, read_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), UUID.randomUUID(), recipientId, UUID.randomUUID(), "community.post-commented.v1",
                java.sql.Timestamp.from(createdAt), readAt == null ? null : java.sql.Timestamp.from(readAt));
    }
}

