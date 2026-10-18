package com.ieltspath.notification.api;

import com.ieltspath.commonsecurity.currentuser.CurrentUser;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.commonsecurity.role.CanonicalRoles;
import com.ieltspath.notification.api.exception.GlobalExceptionHandler;
import com.ieltspath.notification.application.NotificationPreferenceService;
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
        NotificationPreferenceService prefs = new NotificationPreferenceService(jdbc);
        NotificationController controller = new NotificationController(jdbc, users, prefs);
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
}

