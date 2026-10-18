package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.port.PointsGrantPublisher;
import com.ieltspath.learning.application.service.RewardGrantService;
import com.ieltspath.learning.domain.repository.RewardGrantRepository;
import com.ieltspath.learning.domain.repository.RewardGrantRepository.PendingGrant;
import com.ieltspath.learning.infrastructure.scheduler.RewardGrantRelayJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** The points grant relay and the answers from Access, against a real database and a mocked broker. */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "learning.messaging.consumer-enabled=false",
        "learning.mastery.backfill-on-startup=false", "learning.league.close-enabled=false",
        "learning.rewards.relay-enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class RewardGrantRelayIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db").withPassword(UUID.randomUUID().toString());
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired RewardGrantRepository grants;
    @Autowired RewardGrantService service;
    @Autowired RewardGrantRelayJob relay;
    @MockitoBean PointsGrantPublisher publisher;

    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE reward_grants");
        reset(publisher);
    }

    @Test
    void aNewGrantIsSentOnceAndNotAgainBeforeTheResendDelay() {
        grants.enqueue(user, "STREAK_MILESTONE", "2026-10-01:7", 10);

        assertEquals(1, relay.runOnce());
        assertEquals(0, relay.runOnce());

        ArgumentCaptor<PendingGrant> sent = ArgumentCaptor.forClass(PendingGrant.class);
        verify(publisher, times(1)).publish(sent.capture());
        assertEquals(user, sent.getValue().userId());
        assertEquals(10, sent.getValue().amount());
        assertEquals("STREAK_MILESTONE", sent.getValue().reason());
        assertEquals(1, attempts());
    }

    @Test
    void aGrantWithNoAnswerIsSentAgainAfterTheDelay() {
        grants.enqueue(user, "DAILY_QUEST", UUID.randomUUID().toString(), 5);
        relay.runOnce();
        jdbc.update("UPDATE reward_grants SET updated_at = now() - interval '6 minutes'");

        assertEquals(1, relay.runOnce());

        verify(publisher, times(2)).publish(any());
        assertEquals(2, attempts());
    }

    @Test
    void anAnswerSettlesTheGrantAndStopsFurtherSends() {
        grants.enqueue(user, "BADGE", "STREAK_7", 5);
        relay.runOnce();
        UUID id = jdbc.queryForObject("SELECT id FROM reward_grants", UUID.class);

        assertTrue(service.settle(id, "GRANTED", null));
        assertFalse(service.settle(id, "CAPPED", null), "a settled grant is not changed again");
        jdbc.update("UPDATE reward_grants SET updated_at = now() - interval '6 minutes'");

        assertEquals(0, relay.runOnce());
        assertEquals("GRANTED", status());
    }

    @Test
    void cappedAndFailedAnswersAreKept() {
        grants.enqueue(user, "BADGE", "A", 5);
        grants.enqueue(user, "BADGE", "B", 5);
        UUID capped = jdbc.queryForObject("SELECT id FROM reward_grants WHERE reference_id = 'A'", UUID.class);
        UUID failed = jdbc.queryForObject("SELECT id FROM reward_grants WHERE reference_id = 'B'", UUID.class);

        service.settle(capped, "CAPPED", null);
        service.settle(failed, "FAILED", "INVALID_PAYLOAD");

        assertEquals("CAPPED", jdbc.queryForObject("SELECT status FROM reward_grants WHERE id = ?", String.class, capped));
        assertEquals("INVALID_PAYLOAD", jdbc.queryForObject("SELECT last_error FROM reward_grants WHERE id = ?",
                String.class, failed));
    }

    @Test
    void anUnknownStatusChangesNothing() {
        grants.enqueue(user, "BADGE", "A", 5);
        UUID id = jdbc.queryForObject("SELECT id FROM reward_grants", UUID.class);

        assertFalse(service.settle(id, "SOMETHING", null));
        assertFalse(service.settle(id, null, null));

        assertEquals("PENDING", status());
    }

    @Test
    void aGrantSentTooManyTimesWithoutAnAnswerIsGivenUp() {
        grants.enqueue(user, "BADGE", "A", 5);
        jdbc.update("UPDATE reward_grants SET attempts = 10, updated_at = now() - interval '6 minutes'");

        relay.runOnce();

        assertEquals("FAILED", status());
        assertEquals("NO_SETTLEMENT", jdbc.queryForObject("SELECT last_error FROM reward_grants", String.class));
        verify(publisher, never()).publish(any());
    }

    @Test
    void aBrokerOutageLeavesTheGrantDueForTheNextRun() {
        grants.enqueue(user, "BADGE", "A", 5);
        doThrow(new IllegalStateException("broker down")).when(publisher).publish(any());

        assertEquals(0, relay.runOnce());
        assertEquals(0, attempts());
        assertEquals("PENDING", status());

        reset(publisher);
        assertEquals(1, relay.runOnce());
        assertEquals(1, attempts());
    }

    private int attempts() {
        Integer value = jdbc.queryForObject("SELECT attempts FROM reward_grants", Integer.class);
        return value == null ? 0 : value;
    }

    private String status() {
        return jdbc.queryForObject("SELECT status FROM reward_grants", String.class);
    }
}
