package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.usecase.GetStreaksUseCase;
import com.ieltspath.learning.domain.repository.StreakRepository;
import com.ieltspath.learning.domain.vo.StreakState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The streak of many learners in one read, as each of them sees it today, for another service that shows streaks next
 * to profiles. Runs in a transaction that rolls back.
 */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "learning.messaging.consumer-enabled=false",
        "learning.mastery.backfill-on-startup=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class StreakBatchIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db").withPassword(UUID.randomUUID().toString());
    private static final byte[] KEY = new byte[32];
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired GetStreaksUseCase useCase;
    @Autowired StreakRepository streaks;
    @MockitoBean AccessClient access;

    @Test
    void returnsTheCurrentDaysOfEveryLearnerThatHasAStreak() {
        UUID active = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID none = UUID.randomUUID();
        seed(active, 6, today().minusDays(1));
        seed(other, 2, today());

        Map<UUID, Integer> result = useCase.execute(Set.of(active, other, none));

        assertEquals(6, result.get(active));
        assertEquals(2, result.get(other));
        assertFalse(result.containsKey(none), "a learner with no streak row is left out");
    }

    @Test
    void aStreakThatLapsedReadsAsZeroWithoutRewritingTheRow() {
        UUID lapsed = UUID.randomUUID();
        seed(lapsed, 9, today().minusDays(6));

        assertEquals(0, useCase.execute(Set.of(lapsed)).get(lapsed));

        StreakState stored = streaks.find(lapsed).orElseThrow();
        assertEquals(9, stored.currentDays(), "a batch read must not write");
        assertEquals(today().minusDays(6), stored.evaluatedThrough());
    }

    @Test
    void usesTheLearnersOwnTimezoneToDecideWhichDayIsToday() {
        UUID west = UUID.randomUUID();
        UUID east = UUID.randomUUID();
        ZoneId farWest = ZoneId.of("Pacific/Pago_Pago");
        ZoneId farEast = ZoneId.of("Pacific/Kiritimati");
        jdbc.update("INSERT INTO learner_settings (user_id, timezone) VALUES (?, ?)", west, farWest.getId());
        jdbc.update("INSERT INTO learner_settings (user_id, timezone) VALUES (?, ?)", east, farEast.getId());
        // Both were last active on the day before their own today, so both keep a streak of 4.
        seed(west, 4, LocalDate.now(farWest).minusDays(1));
        seed(east, 4, LocalDate.now(farEast).minusDays(1));

        Map<UUID, Integer> result = useCase.execute(Set.of(west, east));

        assertEquals(4, result.get(west));
        assertEquals(4, result.get(east));
    }

    @Test
    void anEmptyBatchReadsNothing() {
        assertTrue(useCase.execute(Set.of()).isEmpty());
        assertTrue(useCase.execute(List.of()).isEmpty());
    }

    private static LocalDate today() {
        return LocalDate.now(DEFAULT_ZONE);
    }

    private void seed(UUID user, int days, LocalDate lastActive) {
        LocalDate start = lastActive.minusDays(days - 1L);
        streaks.save(user, new StreakState(days, days, lastActive, start, lastActive, false, null, null, 0, 0));
    }
}
