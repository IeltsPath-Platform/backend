package com.ieltspath.access.infrastructure.persistence;

import com.ieltspath.access.application.command.ConsumeHumanGradingCreditCommand;
import com.ieltspath.access.application.command.RefundHumanGradingCreditCommand;
import com.ieltspath.access.application.usecase.ConsumeHumanGradingCreditUseCase;
import com.ieltspath.access.domain.exception.DuplicateIdempotencyException;
import com.ieltspath.access.infrastructure.persistence.adapter.JdbcHumanGradingCreditConsumptionRepository;
import com.ieltspath.access.infrastructure.persistence.adapter.OutboxEventRepositoryAdapter;
import com.ieltspath.access.infrastructure.persistence.adapter.SubscriptionRepositoryAdapter;
import com.ieltspath.access.infrastructure.persistence.mapper.AccessPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ConsumeHumanGradingCreditUseCase.class, SubscriptionRepositoryAdapter.class,
        OutboxEventRepositoryAdapter.class, JdbcHumanGradingCreditConsumptionRepository.class,
        AccessPersistenceMapper.class})
class HumanGradingCreditConsumptionIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired ConsumeHumanGradingCreditUseCase credits;
    @Autowired JdbcTemplate jdbc;
    @Autowired TestEntityManager entityManager;

    @Test
    void consumingTwiceForOneSubmissionDebitsOnceAndRefundIsIdempotent() {
        UUID user = UUID.randomUUID();
        UUID subscription = activeSubscription(user, 2);
        UUID submission = UUID.randomUUID();

        assertEquals(1, credits.execute(new ConsumeHumanGradingCreditCommand(user, submission)));
        assertEquals(1, credits.execute(new ConsumeHumanGradingCreditCommand(user, submission)));
        entityManager.flush();
        assertEquals(1, usedCredits(subscription));
        assertEquals(1, count("SELECT count(*) FROM outbox_events WHERE event_type='HumanGradingCreditConsumed'"));

        assertEquals(2, credits.refund(new RefundHumanGradingCreditCommand(user, submission)));
        assertEquals(2, credits.refund(new RefundHumanGradingCreditCommand(user, submission)));
        entityManager.flush();
        assertEquals(0, usedCredits(subscription));
        assertEquals(1, count("SELECT count(*) FROM outbox_events WHERE event_type='HumanGradingCreditRefunded'"));
    }

    @Test
    void submissionIdCannotConsumeAnotherUsersCredit() {
        UUID user = UUID.randomUUID();
        UUID subscription = activeSubscription(user, 2);
        UUID submission = UUID.randomUUID();
        credits.execute(new ConsumeHumanGradingCreditCommand(user, submission));

        assertThrows(DuplicateIdempotencyException.class,
                () -> credits.execute(new ConsumeHumanGradingCreditCommand(UUID.randomUUID(), submission)));
        entityManager.flush();
        assertEquals(1, usedCredits(subscription));
    }

    private UUID activeSubscription(UUID user, int total) {
        UUID subscription = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO subscriptions(id,user_id,plan_id,status,starts_at,source_type,
                    human_grading_credits_total,human_grading_credits_used)
                SELECT ?,?,p.id,'ACTIVE',CURRENT_TIMESTAMP,'ADMIN',?,0 FROM plans p ORDER BY p.created_at LIMIT 1
                """, subscription, user, total);
        return subscription;
    }

    private int usedCredits(UUID subscription) {
        return jdbc.queryForObject("SELECT human_grading_credits_used FROM subscriptions WHERE id=?", Integer.class,
                subscription);
    }

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }
}
