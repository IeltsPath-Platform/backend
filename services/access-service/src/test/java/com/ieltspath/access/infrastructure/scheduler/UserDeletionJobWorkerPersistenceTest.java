package com.ieltspath.access.infrastructure.scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The worker had never run against a database while scheduling was off. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {"spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway", "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ==",
        "spring.rabbitmq.listener.simple.auto-startup=false", "access.user-deletion.poll-delay-ms=3600000"})
class UserDeletionJobWorkerPersistenceTest {
    private static final UUID PLAN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired UserDeletionJobWorker worker;
    @Autowired JdbcTemplate jdbc;

    @Test
    void closesTheAccessOfADeletedUser() {
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        jdbc.update("INSERT INTO subscriptions (id, user_id, plan_id, status, starts_at, ends_at, source_type) "
                + "VALUES (?, ?, ?, 'ACTIVE', now(), now() + interval '30 days', 'ADMIN')", UUID.randomUUID(), userId, PLAN_ID);
        jdbc.update("INSERT INTO point_wallets (user_id, balance, total_credited) VALUES (?, 40, 40)", userId);
        jdbc.update("INSERT INTO user_email_verification_state (user_id, verified, occurred_at, email, display_name) "
                + "VALUES (?, true, now(), 'learner@example.test', 'An')", userId);
        jdbc.update("INSERT INTO deleted_users (user_id) VALUES (?)", userId);
        jdbc.update("INSERT INTO user_deletion_jobs (event_id, user_id) VALUES (?, ?)", eventId, userId);

        worker.processPending();

        assertThat(value("SELECT status FROM user_deletion_jobs WHERE event_id = ?", eventId)).isEqualTo("DONE");
        assertThat(value("SELECT status FROM subscriptions WHERE user_id = ?", userId)).isEqualTo("CANCELLED");
        assertThat(value("SELECT balance::text FROM point_wallets WHERE user_id = ?", userId)).isEqualTo("0");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_email_verification_state WHERE user_id = ?",
                Integer.class, userId)).isZero();
    }

    private String value(String query, UUID id) {
        return jdbc.queryForObject(query, String.class, id);
    }
}
