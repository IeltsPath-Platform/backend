package com.ieltspath.access.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.access.application.command.ActivateKeyCommand;
import com.ieltspath.access.application.usecase.ActivateKeyUseCase;
import com.ieltspath.access.domain.aggregate.ActivationKey;
import com.ieltspath.access.domain.aggregate.KeyProduct;
import com.ieltspath.access.domain.repository.ActivationKeyRepository;
import com.ieltspath.access.domain.repository.KeyProductRepository;
import com.ieltspath.access.domain.vo.ActivationKeyCode;
import com.ieltspath.access.infrastructure.persistence.adapter.ActivationKeyRepositoryAdapter;
import com.ieltspath.access.infrastructure.persistence.adapter.JdbcUserEmailVerificationRepository;
import com.ieltspath.access.infrastructure.persistence.adapter.KeyActivationRepositoryAdapter;
import com.ieltspath.access.infrastructure.persistence.adapter.KeyProductRepositoryAdapter;
import com.ieltspath.access.infrastructure.persistence.adapter.OutboxEventRepositoryAdapter;
import com.ieltspath.access.infrastructure.persistence.adapter.PlanRepositoryAdapter;
import com.ieltspath.access.infrastructure.persistence.adapter.PointLedgerRepositoryAdapter;
import com.ieltspath.access.infrastructure.persistence.adapter.PointWalletRepositoryAdapter;
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

/** Premium keys of one user redeemed at the same moment all succeed and add up on a single active subscription. */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ActivateKeyUseCase.class, ActivationKeyRepositoryAdapter.class, KeyProductRepositoryAdapter.class,
        KeyActivationRepositoryAdapter.class, PointWalletRepositoryAdapter.class, PointLedgerRepositoryAdapter.class,
        SubscriptionRepositoryAdapter.class, PlanRepositoryAdapter.class, OutboxEventRepositoryAdapter.class,
        JdbcUserEmailVerificationRepository.class, AccessPersistenceMapper.class,
        CorrelationRabbitAutoConfiguration.class, ConcurrentSubscriptionGrantIntegrationTest.Json.class})
class ConcurrentSubscriptionGrantIntegrationTest {
    private static final UUID PLAN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final int ACTIVATIONS = 6;

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

    @Autowired ActivateKeyUseCase activation;
    @Autowired KeyProductRepository products;
    @Autowired ActivationKeyRepository keys;
    @Autowired JdbcTemplate jdbc;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void simultaneousActivationsLeaveOneActiveSubscription() throws Exception {
        UUID userId = verifiedUser();
        List<String> codes = premiumCodes(ACTIVATIONS);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(ACTIVATIONS);
        try {
            List<Future<?>> activations = new ArrayList<>();
            for (String code : codes) {
                activations.add(pool.submit(() -> {
                    start.await();
                    return activation.execute(new ActivateKeyCommand(userId, code, "activate-" + code));
                }));
            }
            start.countDown();
            for (Future<?> result : activations) {
                result.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM subscriptions WHERE user_id = ?", Integer.class, userId));
        assertEquals(ACTIVATIONS, jdbc.queryForObject(
                "SELECT human_grading_credits_total FROM subscriptions WHERE user_id = ? AND status = 'ACTIVE'",
                Integer.class, userId));
        assertEquals(ACTIVATIONS, notices(userId));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void replayingAnActivationAnnouncesPremiumOnce() {
        UUID userId = verifiedUser();
        String code = premiumCodes(1).get(0);
        ActivateKeyCommand command = new ActivateKeyCommand(userId, code, "replay-" + code);

        activation.execute(command);
        activation.execute(command);

        assertEquals(1, notices(userId));
    }

    private UUID verifiedUser() {
        UUID userId = UUID.randomUUID();
        jdbc.update("INSERT INTO user_email_verification_state (user_id, verified, occurred_at) "
                + "VALUES (?, true, CURRENT_TIMESTAMP)", userId);
        return userId;
    }

    private List<String> premiumCodes(int count) {
        KeyProduct product = products.save(KeyProduct.createPremiumProduct(
                "PREMIUM-" + UUID.randomUUID(), "Premium 10 ngày", PLAN_ID, 10, 1));
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String raw = ActivationKeyCode.generateRaw();
            ActivationKeyCode code = ActivationKeyCode.fromRawKey(raw);
            keys.save(ActivationKey.create(product.getId(), code.codeHash(), code.codeHint(), null, null));
            codes.add(raw);
        }
        return codes;
    }

    private int notices(UUID userId) {
        return jdbc.queryForObject("SELECT count(*) FROM outbox_events WHERE event_type = 'SubscriptionGranted' "
                + "AND payload ->> 'userId' = ?", Integer.class, userId.toString());
    }
}
