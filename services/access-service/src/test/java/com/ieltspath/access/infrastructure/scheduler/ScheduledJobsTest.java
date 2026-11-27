package com.ieltspath.access.infrastructure.scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards the wiring: a {@code @Scheduled} method does nothing, silently, unless scheduling is enabled. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {"spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway", "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ==",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "access.outbox.relay.enabled=true", "access.outbox.relay.poll-interval=PT1H", "access.subscription-expiry.enabled=true",
        "access.points.reconciliation.enabled=true"})
class ScheduledJobsTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired List<ScheduledTaskHolder> holders;

    @Test
    void theJobsOfTheServiceAreScheduled() {
        var scheduled = holders.stream().flatMap(holder -> holder.getScheduledTasks().stream())
                .map(task -> task.getTask().toString()).toList();

        for (String job : List.of("AccessOutboxRelayScheduler", "OutboxPurgeJob", "SubscriptionLapseScheduler",
                "SubscriptionExpiryNoticeScheduler", "UserDeletionJobWorker", "WalletReconciliationScheduler")) {
            assertThat(scheduled).as(job).anyMatch(task -> task.contains(job));
        }
    }
}
