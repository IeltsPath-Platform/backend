package com.ieltspath.access.infrastructure.persistence;

import com.ieltspath.access.application.usecase.ExpireLapsedSubscriptionsUseCase;
import com.ieltspath.access.application.usecase.SubscriptionExpiryNoticeUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The lapse job and the notice scan run independently, so the scan must still see what the lapse job just marked. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {"spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway", "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ==",
        "spring.rabbitmq.listener.simple.auto-startup=false"})
class SubscriptionExpiryNoticeAfterLapseTest {
    private static final UUID PLAN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired ExpireLapsedSubscriptionsUseCase lapse;
    @Autowired SubscriptionExpiryNoticeUseCase notices;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM subscription_expiry_notices");
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM subscriptions");
    }

    @Test
    void aLongEndedSubscriptionIsNotAnnouncedEvenBeforeItIsMarkedExpired() {
        subscription(learner(), "ACTIVE", Instant.now().minus(Duration.ofDays(30)));

        assertThat(notices.scan(Instant.now())).isZero();
    }

    @Test
    void aRenewedSubscriptionIsAnnouncedAgainForItsNewEnd() {
        Instant now = Instant.now();
        UUID subscriptionId = subscription(learner(), "ACTIVE", now.plus(Duration.ofDays(3)));
        assertThat(notices.scan(now)).isEqualTo(1);
        assertThat(notices.scan(now)).isZero();

        // Extending moves the end date of the same subscription.
        Instant newEnd = now.plus(Duration.ofDays(33));
        jdbc.update("UPDATE subscriptions SET ends_at = ? WHERE id = ?", Timestamp.from(newEnd), subscriptionId);

        assertThat(notices.scan(now)).isZero();
        assertThat(notices.scan(newEnd.minus(Duration.ofDays(3)))).isEqualTo(1);
        assertThat(notices.scan(newEnd.minus(Duration.ofDays(3)))).isZero();
    }

    @Test
    void aLapsedSubscriptionIsAnnouncedOnceUnlessItIsOldOrRenewed() {
        Instant now = Instant.now();
        UUID endedYesterday = subscription(learner(), "ACTIVE", now.minus(Duration.ofDays(1)));
        subscription(learner(), "ACTIVE", now.minus(Duration.ofDays(30)));
        UUID renewedLearner = learner();
        subscription(renewedLearner, "EXPIRED", now.minus(Duration.ofDays(1)));
        subscription(renewedLearner, "ACTIVE", now.plus(Duration.ofDays(60)));
        // This learner has a later subscription, so the earlier one is not announced; the later one was cancelled.
        UUID movedOnLearner = learner();
        subscription(movedOnLearner, "EXPIRED", now.minus(Duration.ofDays(5)));
        subscription(movedOnLearner, "CANCELLED", now.minus(Duration.ofDays(2)));

        lapse.execute(now);

        assertThat(notices.scan(now)).isEqualTo(1);
        assertThat(notices.scan(now)).isZero();
        assertThat(jdbc.queryForList("SELECT aggregate_id FROM outbox_events WHERE event_type = 'SubscriptionExpiryNotice'",
                UUID.class)).containsExactly(endedYesterday);
    }

    private UUID learner() {
        UUID userId = UUID.randomUUID();
        jdbc.update("INSERT INTO user_email_verification_state (user_id, verified, occurred_at, email, display_name) "
                + "VALUES (?, true, now(), 'learner@example.test', 'An')", userId);
        return userId;
    }

    private UUID subscription(UUID userId, String status, Instant endsAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO subscriptions (id, user_id, plan_id, status, starts_at, ends_at, source_type) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 'ADMIN')",
                id, userId, PLAN_ID, status, Timestamp.from(endsAt.minus(Duration.ofDays(30))), Timestamp.from(endsAt));
        return id;
    }
}
