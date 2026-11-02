package com.ieltspath.access.infrastructure.persistence;

import com.ieltspath.access.domain.entity.OutboxEvent;
import com.ieltspath.access.domain.repository.OutboxEventRepository;
import com.ieltspath.commonobservability.CorrelationIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {"spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway", "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ=="})
class OutboxCorrelationPersistenceTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired OutboxEventRepository outbox;
    @Autowired JdbcTemplate jdbc;

    @AfterEach void clearMdc() { MDC.remove(CorrelationIdFilter.MDC_KEY); }

    @Test void storesCorrelationIdOfTheSavingThread() {
        UUID id = UUID.randomUUID();
        MDC.put(CorrelationIdFilter.MDC_KEY, "req-1");
        outbox.save(OutboxEvent.create(id, "Subscription", UUID.randomUUID(), "SubscriptionGranted", "{}"));
        assertEquals("req-1", jdbc.queryForObject("SELECT correlation_id FROM outbox_events WHERE id = ?", String.class, id));
    }

    @Test void storesNullWithoutCorrelationId() {
        UUID id = UUID.randomUUID();
        outbox.save(OutboxEvent.create(id, "Subscription", UUID.randomUUID(), "SubscriptionGranted", "{}"));
        assertNull(jdbc.queryForObject("SELECT correlation_id FROM outbox_events WHERE id = ?", String.class, id));
    }
}
