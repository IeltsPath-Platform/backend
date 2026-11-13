package com.ieltspath.notification.api;

import com.ieltspath.commonsecurity.currentuser.CurrentUser;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.commonsecurity.role.CanonicalRoles;
import com.ieltspath.notification.api.exception.GlobalExceptionHandler;
import com.ieltspath.notification.application.PushDeviceUseCase;
import com.ieltspath.notification.domain.vo.Notification;
import com.ieltspath.notification.infrastructure.persistence.NotificationRepositoryAdapter;
import com.ieltspath.notification.infrastructure.persistence.PushDeliveryRepositoryAdapter;
import com.ieltspath.notification.infrastructure.persistence.PushDeviceRepositoryAdapter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
class PushDeviceIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("filesystem:src/main/resources/db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void cleanUp() {
        jdbc.execute("DELETE FROM notification_deliveries");
        jdbc.execute("DELETE FROM push_devices");
        jdbc.execute("DELETE FROM notifications");
    }

    private MockMvc as(UUID userId) {
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        when(users.requireCurrentUser()).thenReturn(new CurrentUser(userId, Set.of(CanonicalRoles.CUSTOMER)));
        var controller = new PushDeviceController(new PushDeviceUseCase(new PushDeviceRepositoryAdapter(jdbc)), users);
        return MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private void register(UUID user, String installation, String token) throws Exception {
        as(user).perform(put("/api/notifications/devices/" + installation).contentType(MediaType.APPLICATION_JSON)
                .content("{\"platform\":\"ANDROID\",\"pushToken\":\"" + token + "\"}")).andExpect(status().isNoContent());
    }

    private UUID storeNotice(UUID recipient) {
        UUID id = UUID.randomUUID();
        new NotificationRepositoryAdapter(jdbc).insertIfAbsent(id, new Notification(id, recipient, recipient,
                "assessment.grading-completed.v1", null, null, UUID.randomUUID(), Instant.now(), null));
        return id;
    }

    @Test
    void storedNoticeIsQueuedOncePerRegisteredDeviceAndClaimedWithItsToken() throws Exception {
        UUID learner = UUID.randomUUID();
        register(learner, "install-phone-1", "token-a");
        register(learner, "install-phone-1", "token-b");

        UUID notice = storeNotice(learner);

        var claimed = new PushDeliveryRepositoryAdapter(jdbc).claimDue(10, Duration.ofMinutes(2));
        assertEquals(1, claimed.size());
        assertEquals("token-b", claimed.getFirst().token());
        assertEquals(notice, claimed.getFirst().notificationId());
        assertEquals(1, claimed.getFirst().attempts());
        assertTrue(new PushDeliveryRepositoryAdapter(jdbc).claimDue(10, Duration.ofMinutes(2)).isEmpty(),
                "a claimed delivery stays hidden for the lease");
    }

    @Test
    void installationTakenOverByAnotherUserNeverReceivesThePreviousUsersPush() throws Exception {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        register(first, "shared-install", "token-1");
        storeNotice(first);
        register(second, "shared-install", "token-2");

        var claimed = new PushDeliveryRepositoryAdapter(jdbc).claimDue(10, Duration.ofMinutes(2));

        assertEquals(1, claimed.size());
        assertNull(claimed.getFirst().token());
    }

    @Test
    void tokenMovesToTheNewInstallationAndOnlyTheOwnerCanRevoke() throws Exception {
        UUID owner = UUID.randomUUID(), stranger = UUID.randomUUID();
        register(owner, "install-old", "same-token");
        register(owner, "install-new", "same-token");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM push_devices", Integer.class));

        as(stranger).perform(delete("/api/notifications/devices/install-new")).andExpect(status().isNotFound());
        as(owner).perform(delete("/api/notifications/devices/install-new")).andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM push_devices", Integer.class));
    }

    @Test
    void rejectsUnknownPlatformMissingTokenAndMalformedInstallationId() throws Exception {
        UUID user = UUID.randomUUID();
        as(user).perform(put("/api/notifications/devices/install-ok-1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"platform\":\"WEB\",\"pushToken\":\"t\"}")).andExpect(status().isBadRequest());
        as(user).perform(put("/api/notifications/devices/install-ok-1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"platform\":\"IOS\"}")).andExpect(status().isBadRequest());
        as(user).perform(put("/api/notifications/devices/short").contentType(MediaType.APPLICATION_JSON)
                .content("{\"platform\":\"IOS\",\"pushToken\":\"t\"}")).andExpect(status().isBadRequest());
    }
}
