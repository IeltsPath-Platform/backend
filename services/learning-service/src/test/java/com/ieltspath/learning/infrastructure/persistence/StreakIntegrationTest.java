package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.service.GamificationRecorder;
import com.ieltspath.learning.application.service.QuestionEventsAppended;
import com.ieltspath.learning.application.service.StreakRecorder;
import com.ieltspath.learning.application.usecase.StreakUseCase;
import com.ieltspath.learning.domain.repository.StreakRepository;
import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;
import com.ieltspath.learning.domain.vo.QuestionEventSource;
import com.ieltspath.learning.domain.vo.StreakState;
import com.ieltspath.learning.domain.vo.StreakView;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Streak milestones, the freeze and the view, against a real database. Each test seeds a streak that ended yesterday in
 * the default timezone, so one answer today continues it without moving the clock. Runs in a transaction that rolls back.
 */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "learning.messaging.consumer-enabled=false",
        "learning.mastery.backfill-on-startup=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class StreakIntegrationTest {
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
    @Autowired StreakRecorder recorder;
    @Autowired StreakUseCase streak;
    @Autowired StreakRepository streaks;
    @Autowired GamificationRecorder gamification;
    @MockitoBean AccessClient access;

    @Test
    void aWriteWithNoNewAnswerLeavesTheStreakAlone() {
        UUID user = UUID.randomUUID();
        seedStreak(user, 6, yesterday());

        recorder.onAnswers(new QuestionEventsAppended(user));

        assertEquals(6, streaks.find(user).orElseThrow().currentDays());
        assertEquals(0, gamification.progress(user).totalXp());
    }

    @Test
    void anAssessmentResultFinalisedLaterDoesNotCountAsAStudyDay() {
        UUID user = UUID.randomUUID();
        seedStreak(user, 6, yesterday());
        QuestionAttemptEvent graded = new QuestionAttemptEvent(user, UUID.randomUUID(), java.util.List.of(), true, null,
                null, null, null, QuestionEventSource.ASSESSMENT, UUID.randomUUID(), UUID.randomUUID(), true, null);

        recorder.onAnswers(new QuestionEventsAppended(user, java.util.List.of(graded)));

        assertEquals(6, streaks.find(user).orElseThrow().currentDays());
        assertEquals(yesterday(), streaks.find(user).orElseThrow().lastActive());
        assertEquals(0, gamification.progress(user).totalXp());
    }

    @Test
    void aSeventhDayPaysOneHundredXpAndQueuesTenPointsOnce() {
        UUID user = UUID.randomUUID();
        seedStreak(user, 6, yesterday());

        recorder.onAnswers(answered(user));
        recorder.onAnswers(answered(user));

        assertEquals(7, streaks.find(user).orElseThrow().currentDays());
        assertEquals(100, gamification.progress(user).totalXp(), "the milestone XP, paid once");
        assertEquals(1, pendingPoints(user, 10), "the weekly points, queued once");
    }

    @Test
    void theThirtiethDayPaysFiveHundredXpAndNoPoints() {
        UUID user = UUID.randomUUID();
        seedStreak(user, 29, yesterday());

        recorder.onAnswers(answered(user));

        assertEquals(30, streaks.find(user).orElseThrow().currentDays());
        assertEquals(500, gamification.progress(user).totalXp());
        assertEquals(0, pendingPoints(user, 10), "only weekly milestones pay points");
    }

    @Test
    void aFreezeIsChargedAtTheCurrentPriceAndOnlyOneIsOwnedAtATime() {
        UUID user = UUID.randomUUID();
        UUID request = UUID.randomUUID();
        seedStreak(user, 3, yesterday());

        StreakView bought = streak.buyFreeze(user, request);

        assertTrue(bought.freezeAvailable());
        verify(access).debit(eq(user), eq(10), eq("STREAK_FREEZE"), eq(request), eq(request.toString()), anyString());
        LearningRequestException again = assertThrows(LearningRequestException.class,
                () -> streak.buyFreeze(user, UUID.randomUUID()));
        assertEquals(409, again.getStatus());
        assertEquals("FREEZE_ALREADY_OWNED", again.getCode());
    }

    @Test
    void aFreezeNeedsAStreakToProtect() {
        UUID user = UUID.randomUUID();

        LearningRequestException error = assertThrows(LearningRequestException.class,
                () -> streak.buyFreeze(user, UUID.randomUUID()));

        assertEquals("NO_STREAK_TO_PROTECT", error.getCode());
        verify(access, never()).debit(any(), eq(10), anyString(), any(), anyString(), anyString());
    }

    @Test
    void aStreakLostWhileAwayIsShownAsLostWithItsLength() {
        UUID user = UUID.randomUUID();
        seedStreak(user, 4, yesterday().minusDays(1));

        StreakView view = streak.view(user);

        assertEquals(0, view.currentDays());
        assertEquals(4, view.lostStreakDays());
        assertFalse(view.activeToday());
    }

    private static LocalDate yesterday() {
        return LocalDate.now(DEFAULT_ZONE).minusDays(1);
    }

    private void seedStreak(UUID user, int days, LocalDate lastActive) {
        LocalDate start = lastActive.minusDays(days - 1L);
        streaks.save(user, new StreakState(days, days, lastActive, start, lastActive, false, null, null, 0, 0));
    }

    private int pendingPoints(UUID user, int amount) {
        return jdbc.queryForObject("SELECT count(*) FROM reward_grants WHERE user_id = ? AND reason = "
                + "'STREAK_MILESTONE' AND amount = ? AND status = 'PENDING'", Integer.class, user, amount);
    }

    /** One new answer the learner submitted in a lesson, as a write of the event log reports it. */
    private static QuestionEventsAppended answered(UUID user) {
        return new QuestionEventsAppended(user, java.util.List.of(QuestionAttemptEvent.of(user, UUID.randomUUID(),
                java.util.List.of(), true, null, null, QuestionEventSource.LESSON_EXERCISE, UUID.randomUUID(), true)));
    }
}
