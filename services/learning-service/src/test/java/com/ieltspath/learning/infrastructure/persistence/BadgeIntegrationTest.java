package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.service.BadgeService;
import com.ieltspath.learning.application.service.QuestionEventsAppended;
import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;
import com.ieltspath.learning.domain.vo.QuestionEventSource;
import com.ieltspath.learning.application.service.StreakRecorder;
import com.ieltspath.learning.application.usecase.BadgesUseCase;
import com.ieltspath.learning.domain.repository.StreakRepository;
import com.ieltspath.learning.domain.vo.StreakState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Badges are awarded once, only while switched on, and show their progress, against a real database. Rolls back. */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "learning.messaging.consumer-enabled=false",
        "learning.mastery.backfill-on-startup=false", "learning.league.close-enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class BadgeIntegrationTest {
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
    @Autowired BadgeService service;
    @Autowired BadgesUseCase badges;
    @Autowired StreakRecorder streakRecorder;
    @Autowired StreakRepository streaks;

    @Test
    void aSevenDayStreakAwardsTheBadgeOnceWithItsPoints() {
        UUID user = UUID.randomUUID();
        LocalDate yesterday = LocalDate.now(DEFAULT_ZONE).minusDays(1);
        streaks.save(user, new StreakState(6, 6, yesterday, yesterday.minusDays(5), yesterday, false, null, null, 0,
                0));

        streakRecorder.onAnswers(answered(user));
        streakRecorder.onAnswers(answered(user));

        assertEquals(List.of("STREAK_7"), held(user));
        assertEquals(1, count("SELECT count(*) FROM reward_grants WHERE user_id = '" + user
                + "' AND reason = 'BADGE' AND reference_id = 'STREAK_7' AND amount = 5"));
    }

    @Test
    void aRedeliveredCoursePassAwardsTheBadgeOnlyOnce() {
        UUID user = UUID.randomUUID();

        service.onCoursePassed(user);
        service.onCoursePassed(user);

        assertEquals(List.of("COURSE_PASSED"), held(user));
        assertEquals(1, count("SELECT count(*) FROM reward_grants WHERE user_id = '" + user + "' AND reason = 'BADGE'"));
    }

    @Test
    void aSwitchedOffBadgeIsNotAwarded() {
        UUID user = UUID.randomUUID();
        badges.setActive("STREAK_30", false);

        service.onStreak(user, 30);

        assertEquals(List.of("STREAK_7"), held(user));
        badges.setActive("STREAK_30", true);
    }

    @Test
    void tenMaintainedPointsAwardTheMaintainBadge() {
        UUID user = UUID.randomUUID();
        jdbc.update("INSERT INTO kp_mastery_profiles (user_id, knowledge_point_id, mastery, tier, evidence_count, "
                + "last_evidence_at) SELECT ?, gen_random_uuid(), 0.9, 'MAINTAIN', 5, now() FROM generate_series(1, 10)",
                user);

        service.onMaintained(user);

        assertEquals(List.of("KP_MAINTAIN_10"), held(user));
    }

    @Test
    void thirtyCompletedQuestsAwardTheQuestBadge() {
        UUID user = UUID.randomUUID();
        jdbc.update("INSERT INTO daily_quests (id, user_id, local_date, type, required, progress, completed_at, "
                + "xp_reward) SELECT gen_random_uuid(), ?, current_date - g, 'PRACTICE', 5, 5, now(), 20 "
                + "FROM generate_series(1, 30) g", user);

        service.onQuestCompleted(user);

        assertEquals(List.of("QUESTS_30"), held(user));
    }

    @Test
    void levelTenAndTheFirstTopicTestAwardTheirBadges() {
        UUID user = UUID.randomUUID();

        service.onLevel(user, 9);
        assertEquals(List.of(), held(user));
        service.onLevel(user, 10);
        service.onTopicTest(user);

        assertEquals(List.of("FIRST_TOPIC_TEST", "LEVEL_10"), held(user));
    }

    @Test
    void theListShowsProgressForCountedBadgesAndHidesSwitchedOffOnesNotHeld() {
        UUID user = UUID.randomUUID();
        jdbc.update("INSERT INTO kp_mastery_profiles (user_id, knowledge_point_id, mastery, tier, evidence_count, "
                + "last_evidence_at) SELECT ?, gen_random_uuid(), 0.9, 'MAINTAIN', 5, now() FROM generate_series(1, 4)",
                user);
        badges.setActive("STREAK_100", false);

        List<BadgesUseCase.Item> items = badges.mine(user);

        assertTrue(items.stream().noneMatch(item -> item.code().equals("STREAK_100")));
        BadgesUseCase.Item maintain = items.stream().filter(item -> item.code().equals("KP_MAINTAIN_10")).findFirst()
                .orElseThrow();
        assertFalse(maintain.awarded());
        assertEquals(4, maintain.progress().current());
        assertEquals(10, maintain.progress().target());
        assertNull(items.stream().filter(item -> item.code().equals("COURSE_PASSED")).findFirst().orElseThrow()
                .progress(), "a badge that is not counted has no progress");
    }

    @Test
    void anUnknownBadgeCannotBeSwitched() {
        LearningRequestException error = assertThrows(LearningRequestException.class,
                () -> badges.setActive("NO_SUCH_BADGE", false));

        assertEquals(404, error.getStatus());
        assertEquals("BADGE_NOT_FOUND", error.getCode());
    }

    private List<String> held(UUID user) {
        return jdbc.queryForList("SELECT badge_code FROM learner_badges WHERE user_id = ? ORDER BY badge_code",
                String.class, user);
    }

    private int count(String sql) {
        Integer value = jdbc.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    /** One new answer the learner submitted in a lesson, as a write of the event log reports it. */
    private static QuestionEventsAppended answered(UUID user) {
        return new QuestionEventsAppended(user, java.util.List.of(QuestionAttemptEvent.of(user, UUID.randomUUID(),
                java.util.List.of(), true, null, null, QuestionEventSource.LESSON_EXERCISE, UUID.randomUUID(), true)));
    }
}
