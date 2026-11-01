package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.command.GrantSubscriptionCommand;
import com.ieltspath.access.application.exception.InvalidIdempotencyKeyException;
import com.ieltspath.access.domain.aggregate.Plan;
import com.ieltspath.access.domain.exception.DuplicateIdempotencyException;
import com.ieltspath.access.domain.repository.PlanRepository;
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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false", "spring.config.import=", "eureka.client.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway",
        "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ=="
})
class GrantSubscriptionIdempotencyTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired GrantSubscriptionUseCase grant;
    @Autowired PlanRepository plans;
    @Autowired JdbcTemplate jdbc;
    private Plan plan;
    private UUID userId;

    @BeforeEach
    void createPlan() {
        plan = plans.save(Plan.create("IDEMPOTENT-" + UUID.randomUUID(), "Idempotent grant"));
        userId = UUID.randomUUID();
    }

    @Test
    void sameKeySamePayloadReturnsFirstResultWithoutExtending() {
        String key = UUID.randomUUID().toString();
        var first = grant.execute(command(key, 30));
        var replay = grant.execute(command(key, 30));

        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(replay.endsAt()).isEqualTo(first.endsAt());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM subscriptions WHERE user_id = ?", Integer.class, userId))
                .isEqualTo(1);
    }

    @Test
    void sameKeyDifferentPayloadIsConflict() {
        String key = UUID.randomUUID().toString();
        grant.execute(command(key, 30));

        assertThatThrownBy(() -> grant.execute(command(key, 31)))
                .isInstanceOf(DuplicateIdempotencyException.class);
    }

    @Test
    void missingKeyIsBadRequest() {
        assertThatThrownBy(() -> grant.execute(command(" ", 30)))
                .isInstanceOf(InvalidIdempotencyKeyException.class);
    }

    @Test
    void concurrentSameKeyGrantsOnce() throws Exception {
        String key = UUID.randomUUID().toString();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = CompletableFuture.supplyAsync(() -> grant.execute(command(key, 30)), pool);
            var second = CompletableFuture.supplyAsync(() -> grant.execute(command(key, 30)), pool);
            int successes = 0;
            for (var result : new CompletableFuture[]{first, second}) {
                try { result.join(); successes++; }
                catch (RuntimeException expectedConflict) { }
            }
            assertThat(successes).isBetween(1, 2);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_grant_requests WHERE idempotency_key = ?",
                Integer.class, key)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM subscriptions WHERE user_id = ?", Integer.class, userId))
                .isEqualTo(1);
    }

    @Test
    void replayWritesNoSecondOutboxEvent() {
        String key = UUID.randomUUID().toString();
        var first = grant.execute(command(key, 30));
        grant.execute(command(key, 30));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events WHERE aggregate_id = ? AND event_type = ?",
                Integer.class, first.id(), "SubscriptionGranted")).isEqualTo(1);
    }

    private GrantSubscriptionCommand command(String key, int durationDays) {
        return new GrantSubscriptionCommand(userId, plan.getId(), durationDays, 2, key);
    }
}
