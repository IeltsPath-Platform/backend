package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.service.GamificationRecorder;
import com.ieltspath.learning.application.service.HeartsService;
import com.ieltspath.learning.application.usecase.HeartsUseCase;
import com.ieltspath.learning.domain.exception.HeartsDepletedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Hearts against a real database, with Access stubbed: depletion, refill charged once, PREMIUM and level-up refills. */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "learning.messaging.consumer-enabled=false",
        "learning.mastery.backfill-on-startup=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class HeartsIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db").withPassword(UUID.randomUUID().toString());
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired HeartsService hearts;
    @Autowired HeartsUseCase heartsUseCase;
    @Autowired GamificationRecorder gamification;
    @MockitoBean AccessClient access;
    @MockitoBean LearningContentClient content;

    @BeforeEach
    void notPremiumByDefault() {
        when(access.premium()).thenReturn(false);
        when(access.debit(any(), anyInt(), anyString(), any(), anyString(), anyString())).thenReturn(UUID.randomUUID());
    }

    @Test
    void aLearnerWithNoHeartsIsRefusedWithTheNextHeartAndTheRefillCost() {
        UUID user = UUID.randomUUID();
        hearts.lose(user, 5);

        HeartsDepletedException error = assertThrows(HeartsDepletedException.class, () -> hearts.requireHearts(user));

        assertEquals(5, error.getRefillCost());
        Instant next = error.getNextHeartAt();
        assertTrue(Duration.between(Instant.now(), next).toHours() >= 4, "the next heart is about five hours away");
    }

    @Test
    void aRefillChargesPointsOnceUnderItsRequestIdAndAFullLearnerCannotRefillAgain() {
        UUID user = UUID.randomUUID();
        UUID request = UUID.randomUUID();
        hearts.lose(user, 2);

        HeartsService.Snapshot after = heartsUseCase.refill(user, request);

        assertEquals(5, after.hearts());
        verify(access, times(1)).debit(eq(user), eq(5), eq("HEART_REFILL"), eq(request), eq(request.toString()),
                anyString());
        LearningRequestException again = assertThrows(LearningRequestException.class,
                () -> heartsUseCase.refill(user, request));
        assertEquals("HEARTS_FULL", again.getCode());
        verify(access, times(1)).debit(any(), anyInt(), anyString(), any(), anyString(), anyString());
    }

    @Test
    void aPremiumLearnerHasSevenHearts() {
        when(access.premium()).thenReturn(true);
        UUID user = UUID.randomUUID();

        assertEquals(7, hearts.snapshot(user).maxHearts());
    }

    @Test
    void aLevelUpRefillsTheHeartsForFree() {
        UUID user = UUID.randomUUID();
        hearts.lose(user, 3);
        List<GamificationRecorder.Rise> rises = new ArrayList<>();
        for (int index = 0; index < 20; index++) rises.add(new GamificationRecorder.Rise(UUID.randomUUID(), true));

        gamification.recordRises(user, rises);

        assertEquals(5, hearts.snapshot(user).hearts());
        verify(access, never()).debit(any(), anyInt(), anyString(), any(), anyString(), anyString());
    }
}
