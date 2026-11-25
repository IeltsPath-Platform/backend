package com.ieltspath.user.infrastructure.persistence;

import com.ieltspath.user.domain.aggregate.AuthAuditEvent;
import com.ieltspath.user.infrastructure.adapter.AuthAuditEventRepositoryAdapter;
import com.ieltspath.user.infrastructure.persistence.entity.AuthAuditEventJpaEntity;
import com.ieltspath.user.infrastructure.persistence.repository.AuthAuditEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class AuthAuditEventPersistenceTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.flyway.enabled", () -> true);
    }

    @Autowired AuthAuditEventJpaRepository repository;
    @Autowired JdbcTemplate jdbc;

    @Test
    void persistsJsonbAndPagesNewestEventsWithStableOrdering() {
        UUID userId = UUID.randomUUID();
        LocalDateTime old = LocalDateTime.now().minusMinutes(1);
        repository.save(AuthAuditEventJpaEntity.builder().id(UUID.randomUUID()).userId(userId).eventType("SESSION_REVOKED")
                .detail(Map.of("reason", "USER_REQUEST")).createdAt(old).build());
        UUID newestId = UUID.randomUUID();
        repository.save(AuthAuditEventJpaEntity.builder().id(newestId).userId(userId).eventType("PASSWORD_CHANGED")
                .detail(Map.of("method", "PASSWORD")).createdAt(old.plusSeconds(10)).build());

        var page = repository.findByUserId(userId, org.springframework.data.domain.PageRequest.of(0, 1,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
                        .and(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(newestId);
        assertThat(jdbc.queryForObject("SELECT detail->>'method' FROM auth_audit_events WHERE id = ?", String.class, newestId))
                .isEqualTo("PASSWORD");
    }

    @Test
    void deletesOnlyExpiredRowsInTheBoundedSkipLockedBatch() {
        UUID userId = UUID.randomUUID();
        jdbc.update("INSERT INTO auth_audit_events (id, user_id, event_type, created_at) VALUES (?, ?, ?, ?), (?, ?, ?, ?)",
                UUID.randomUUID(), userId, "SESSION_REVOKED", LocalDateTime.now().minusDays(400),
                UUID.randomUUID(), userId, "SESSION_REVOKED", LocalDateTime.now().minusDays(1));

        int removed = repository.deleteExpiredBatch(LocalDateTime.now().minusDays(365), 1);

        assertThat(removed).isEqualTo(1);
        assertThat(repository.findByUserId(userId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements())
                .isEqualTo(1);
    }

    @Test
    void persistsAuditEventWhenCorrelationIdExceedsTheColumnLimit() {
        UUID eventId = UUID.randomUUID();
        String correlationId = "b".repeat(100);
        var adapter = new AuthAuditEventRepositoryAdapter(repository);

        adapter.save(new AuthAuditEvent(eventId, UUID.randomUUID(), "SESSION_REVOKED", null, null,
                correlationId, Map.of(), LocalDateTime.now()));

        var persisted = repository.findById(eventId).orElseThrow();
        assertThat(persisted.getCorrelationId()).hasSize(64).isEqualTo("b".repeat(64));
    }
}
