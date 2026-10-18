package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.service.GamificationRecorder;
import com.ieltspath.learning.application.service.LearnerTimezones;
import com.ieltspath.learning.application.service.MasteryProfileUpdater;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.GamificationSummary;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

/** XP from submissions and milestones, the daily cap, the level and the learner timezone, against a real database. */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "learning.messaging.consumer-enabled=false",
        "learning.mastery.backfill-on-startup=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class GamificationIntegrationTest {
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
    @Autowired GamificationRecorder gamification;
    @Autowired LearnerTimezones timezones;
    @Autowired KnowledgeEvidenceRepository evidence;
    @Autowired MasteryProfileUpdater masteryProfiles;
    @MockitoBean LearningContentClient content;

    @BeforeEach
    void everyQuestionIsHard() {
        when(content.questionDifficulties(anyCollection())).thenAnswer(invocation -> {
            Collection<UUID> ids = invocation.getArgument(0);
            Map<UUID, String> difficulties = new HashMap<>();
            ids.forEach(id -> difficulties.put(id, "HARD"));
            return difficulties;
        });
    }

    @Test
    void twelveHardAnswersInADayCountOneHundredAndShadowTheRest() {
        UUID user = UUID.randomUUID();
        List<UUID> correct = questions(12);

        GamificationSummary summary = gamification.recordCorrectAnswers(user, UUID.randomUUID(), correct);

        assertEquals(100, summary.xpEarned());
        assertTrue(summary.xpCapped());
        assertEquals(100, summary.totalXp());
        assertEquals(1, summary.level());
        assertEquals(140, jdbc.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM xp_ledger "
                + "WHERE user_id = ? AND NOT counted", Integer.class, user));
        assertEquals(100, gamification.progress(user).xpToday());
    }

    @Test
    void aRepeatedRequestAddsNothing() {
        UUID user = UUID.randomUUID();
        UUID request = UUID.randomUUID();
        List<UUID> correct = questions(2);
        gamification.recordCorrectAnswers(user, request, correct);

        GamificationSummary repeat = gamification.recordCorrectAnswers(user, request, correct);

        assertEquals(0, repeat.xpEarned());
        assertEquals(40, repeat.totalXp());
    }

    @Test
    void aKnowledgePointPaysEachMilestoneOnce() {
        UUID user = UUID.randomUUID();
        UUID point = UUID.randomUUID();

        gamification.recordRises(user, List.of(new GamificationRecorder.Rise(point, true)));
        gamification.recordRises(user, List.of(new GamificationRecorder.Rise(point, true)));

        assertEquals(75, gamification.progress(user).totalXp(), "25 for consolidate plus 50 for maintain, once");
    }

    @Test
    void aPointAtNinetyPercentEarnsFivePointsOnceAndOneBelowEarnsNone() {
        UUID user = UUID.randomUUID();
        UUID mastered = UUID.randomUUID();
        UUID almost = UUID.randomUUID();

        gamification.recordRises(user, List.of(new GamificationRecorder.Rise(mastered, true, 0.93),
                new GamificationRecorder.Rise(almost, true, 0.89)));
        gamification.recordRises(user, List.of(new GamificationRecorder.Rise(mastered, true, 0.97)));

        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM reward_grants WHERE user_id = ?", Integer.class, user));
        assertEquals(5, jdbc.queryForObject("SELECT amount FROM reward_grants WHERE user_id = ? AND reason = 'KP_MASTERED' "
                + "AND reference_id = ? AND status = 'PENDING'", Integer.class, user, mastered.toString()));
    }

    @Test
    void onlyPracticeEvidenceEarnsMilestoneXp() {
        UUID user = UUID.randomUUID();
        UUID diagnosed = UUID.randomUUID();
        UUID practiced = UUID.randomUUID();
        UUID topic = UUID.randomUUID();
        jdbc.update("INSERT INTO knowledge_point_catalog (kp_id, topic_id, has_practice_set) "
                + "VALUES (?, ?, TRUE), (?, ?, TRUE)", diagnosed, topic, practiced, topic);

        // Three right answers reach maintain, but a diagnostic probe is not practice, so it pays nothing.
        List<KnowledgeEvidence> probes = correctEvidence(diagnosed, EvidenceSource.DIAGNOSTIC_PROBE, 3);
        evidence.append(user, probes);
        masteryProfiles.recordEvidence(user, probes);
        assertEquals(0, gamification.progress(user).totalXp());

        List<KnowledgeEvidence> reviews = correctEvidence(practiced, EvidenceSource.REVIEW_SET, 3);
        evidence.append(user, reviews);
        masteryProfiles.recordEvidence(user, reviews);
        assertEquals(75, gamification.progress(user).totalXp());
    }

    private static List<KnowledgeEvidence> correctEvidence(UUID kp, EvidenceSource source, int count) {
        List<KnowledgeEvidence> rows = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            rows.add(KnowledgeEvidence.of(kp, true, source, UUID.randomUUID()));
        }
        return rows;
    }

    @Test
    void reachingTheNextLevelIsReportedWithTheXpLeftToIt() {
        UUID user = UUID.randomUUID();
        List<GamificationRecorder.Rise> rises = new ArrayList<>();
        for (int index = 0; index < 20; index++) rises.add(new GamificationRecorder.Rise(UUID.randomUUID(), true));

        gamification.recordRises(user, rises);

        GamificationRecorder.Progress progress = gamification.progress(user);
        assertEquals(1500, progress.totalXp());
        assertEquals(2, progress.level());
        assertEquals(2000, progress.nextLevelAt());
        assertEquals(500, progress.xpToNextLevel());
    }

    @Test
    void theDailyCapFollowsTheLearnersLocalDay() {
        UUID user = UUID.randomUUID();
        timezones.save(user, "Etc/GMT+12");
        gamification.recordCorrectAnswers(user, UUID.randomUUID(), questions(5));

        GamificationSummary atTheCap = gamification.recordCorrectAnswers(user, UUID.randomUUID(), questions(1));
        assertEquals(0, atTheCap.xpEarned(), "five hard answers filled the 100 XP of this local day");
        assertTrue(atTheCap.xpCapped());

        // UTC+14 is 26 hours ahead of UTC-12, so it is always on a later calendar day: the cap starts again there.
        timezones.save(user, "Pacific/Kiritimati");
        GamificationSummary nextDay = gamification.recordCorrectAnswers(user, UUID.randomUUID(), questions(1));

        assertEquals(20, nextDay.xpEarned());
        assertFalse(nextDay.xpCapped());
    }

    @Test
    void theLearnerTimezoneIsSavedAndAnInvalidOneIsRefused() {
        UUID user = UUID.randomUUID();
        assertEquals("Asia/Ho_Chi_Minh", timezones.timezoneOf(user), "the default before any choice");

        timezones.save(user, "Europe/Paris");
        assertEquals("Europe/Paris", timezones.timezoneOf(user));

        LearningRequestException error = assertThrows(LearningRequestException.class,
                () -> timezones.save(user, "UTC+3"));
        assertEquals(400, error.getStatus());
        assertEquals("INVALID_TIMEZONE", error.getCode());
        assertEquals("Europe/Paris", timezones.timezoneOf(user), "a refused value changes nothing");
    }

    private static List<UUID> questions(int count) {
        List<UUID> ids = new ArrayList<>();
        for (int index = 0; index < count; index++) ids.add(UUID.randomUUID());
        return ids;
    }
}
