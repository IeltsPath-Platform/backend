package com.ieltspath.access.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.access.application.command.GrantSubscriptionCommand;
import com.ieltspath.access.application.usecase.GrantSubscriptionUseCase;
import com.ieltspath.access.infrastructure.persistence.adapter.JdbcAdminGrantRequestRepository;
import com.ieltspath.access.infrastructure.persistence.adapter.OutboxEventRepositoryAdapter;
import com.ieltspath.access.infrastructure.persistence.adapter.PlanRepositoryAdapter;
import com.ieltspath.access.infrastructure.persistence.adapter.SubscriptionRepositoryAdapter;
import com.ieltspath.access.infrastructure.persistence.mapper.AccessPersistenceMapper;
import com.ieltspath.commonobservability.CorrelationRabbitAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Grants for one user that run at the same moment all succeed and add up on a single active subscription. */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GrantSubscriptionUseCase.class, SubscriptionRepositoryAdapter.class, PlanRepositoryAdapter.class,
        OutboxEventRepositoryAdapter.class, JdbcAdminGrantRequestRepository.class, AccessPersistenceMapper.class,
        CorrelationRabbitAutoConfiguration.class, ConcurrentSubscriptionGrantIntegrationTest.Json.class})
class ConcurrentSubscriptionGrantIntegrationTest {
    private static final UUID PLAN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final int GRANTS = 6;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Json {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired GrantSubscriptionUseCase grant;
    @Autowired JdbcTemplate jdbc;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void simultaneousGrantsLeaveOneActiveSubscription() throws Exception {
        UUID userId = UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(GRANTS);
        try {
            List<Future<?>> grants = new ArrayList<>();
            for (int i = 0; i < GRANTS; i++) {
                grants.add(pool.submit(() -> {
                    start.await();
                    return grant.execute(new GrantSubscriptionCommand(userId, PLAN_ID, 10, 1));
                }));
            }
            start.countDown();
            for (Future<?> result : grants) {
                result.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM subscriptions WHERE user_id = ?", Integer.class, userId));
        assertEquals(GRANTS, jdbc.queryForObject(
                "SELECT human_grading_credits_total FROM subscriptions WHERE user_id = ? AND status = 'ACTIVE'",
                Integer.class, userId));
    }
}
