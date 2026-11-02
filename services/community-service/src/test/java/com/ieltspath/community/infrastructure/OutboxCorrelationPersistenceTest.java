package com.ieltspath.community.infrastructure;

import com.ieltspath.commonobservability.CorrelationIdFilter;
import com.ieltspath.community.application.port.CommunityEventOutbox;
import com.ieltspath.events.community.CommunityEventTypes;
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
        "spring.rabbitmq.listener.simple.auto-startup=false", "community.outbox.relay.enabled=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway", "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ=="})
class OutboxCorrelationPersistenceTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired CommunityEventOutbox outbox;
    @Autowired JdbcTemplate jdbc;
    @AfterEach void clearMdc() { MDC.remove(CorrelationIdFilter.MDC_KEY); }

    @Test void storesCorrelationIdOfTheSavingThread() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "req-1");
        outbox.record(CommunityEventTypes.POST_COMMENTED, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null);
        assertEquals("req-1", jdbc.queryForObject("SELECT correlation_id FROM outbox_events ORDER BY created_at DESC LIMIT 1", String.class));
    }

    @Test void storesNullWithoutCorrelationId() {
        outbox.record(CommunityEventTypes.POST_COMMENTED, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null);
        assertNull(jdbc.queryForObject("SELECT correlation_id FROM outbox_events ORDER BY created_at DESC LIMIT 1", String.class));
    }
}
