package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.QuestionEventLog;
import com.ieltspath.learning.application.service.GamificationRecorder;
import com.ieltspath.learning.application.service.LearnerTimezones;
import com.ieltspath.learning.application.usecase.GetQuestsUseCase;
import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;
import com.ieltspath.learning.domain.vo.QuestionEventSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** Daily quests: made on first use, advanced by new answers only, and paid once, against a real database. */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "learning.messaging.consumer-enabled=false",
        "learning.mastery.backfill-on-startup=false", "learning.league.close-enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class QuestIntegrationTest {
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
    @Autowired GetQuestsUseCase quests;
    @Autowired QuestionEventLog events;
    @Autowired GamificationRecorder gamification;
    @Autowired LearnerTimezones timezones;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean LearningContentClient content;

    private final UUID weakPoint = UUID.randomUUID();

    @BeforeEach
    void contentHasOnePracticablePoint() {
        LearningContentClient.KnowledgePoint point = new LearningContentClient.KnowledgePoint(weakPoint, "R60_TFNG",
                "True false not given", "STRATEGY", "READING", null, true);
        when(content.getTopicSequence()).thenReturn(List.of(new LearningContentClient.Topic(UUID.randomUUID(), "T1",
                "Topic", 1, null, List.of(point), null, false, null, null, null)));
    }

    @Test
    void theDayStartsWithThreeQuestsAndALongerPracticeWhenNoPointIsWeak() {
        GetQuestsUseCase.QuestsView view = inTransaction(() -> quests.view(UUID.randomUUID()));

        assertEquals(List.of("PRACTICE", "PRACTICE_PLUS", "STREAK"),
                view.quests().stream().map(GetQuestsUseCase.Item::type).toList());
        assertEquals(0, view.completed());
        assertEquals(10, view.quests().get(1).required());
    }

    @Test
    void aWeakPointWithPracticeBecomesTheTargetOfTheWeaknessQuest() {
        UUID user = UUID.randomUUID();
        profile(user, weakPoint, "PRIORITY", 0.3);

        GetQuestsUseCase.QuestsView view = inTransaction(() -> quests.view(user));

        assertEquals(List.of("PRACTICE", "WEAKNESS", "STREAK"),
                view.quests().stream().map(GetQuestsUseCase.Item::type).toList());
        assertEquals(weakPoint, view.quests().get(1).target().knowledgePointId());
        assertEquals("R60_TFNG", view.quests().get(1).target().code());
    }

    @Test
    void aWeakPointNobodyCanPracticeLeavesNoTarget() {
        UUID user = UUID.randomUUID();
        profile(user, UUID.randomUUID(), "PRIORITY", 0.1);

        GetQuestsUseCase.QuestsView view = inTransaction(() -> quests.view(user));

        assertEquals("PRACTICE_PLUS", view.quests().get(1).type());
    }

    @Test
    void fiveLessonAnswersCompleteThePracticeQuestOnceAndAReplayPaysNothingMore() {
        UUID user = UUID.randomUUID();
        UUID reference = UUID.randomUUID();
        List<QuestionAttemptEvent> batch = answers(user, QuestionEventSource.LESSON_EXERCISE, reference, 5, List.of());

        inTransaction(() -> quests.view(user));
        inTransaction(() -> { events.append(batch); return null; });
        inTransaction(() -> { events.append(batch); return null; });

        GetQuestsUseCase.QuestsView view = inTransaction(() -> quests.view(user));
        assertEquals(List.of(true, false, true), view.quests().stream().map(GetQuestsUseCase.Item::completed).toList());
        assertEquals(30, gamification.progress(user).totalXp(), "20 for practice and 10 for the streak, once each");
        assertEquals(1, count("SELECT count(*) FROM reward_grants WHERE user_id = '" + user
                + "' AND reason = 'DAILY_QUEST' AND amount = 5"));
    }

    @Test
    void threeAnswersOnTheTargetPointCompleteTheWeaknessQuest() {
        UUID user = UUID.randomUUID();
        profile(user, weakPoint, "CONSOLIDATE", 0.6);
        inTransaction(() -> quests.view(user));

        inTransaction(() -> {
            events.append(answers(user, QuestionEventSource.FREE_PRACTICE, UUID.randomUUID(), 3, List.of(weakPoint)));
            return null;
        });

        GetQuestsUseCase.QuestsView view = inTransaction(() -> quests.view(user));
        GetQuestsUseCase.Item weakness = view.quests().get(1);
        assertEquals("WEAKNESS", weakness.type());
        assertTrue(weakness.completed());
        assertEquals(40, gamification.progress(user).totalXp(), "30 for the weakness quest and 10 for the streak");
    }

    @Test
    void examAnswersCountNeitherAsPracticeNorAsActivity() {
        UUID user = UUID.randomUUID();
        inTransaction(() -> quests.view(user));

        inTransaction(() -> { events.append(exam(user, 5)); return null; });

        GetQuestsUseCase.QuestsView view = inTransaction(() -> quests.view(user));
        assertEquals(0, view.quests().get(0).progress());
        assertFalse(view.quests().get(2).completed(), "a result finalised by grading says nothing about today's study");
    }

    @Test
    void anExamResultAppliedByTheConsumerMakesNoQuests() {
        UUID user = UUID.randomUUID();

        inTransaction(() -> { events.append(exam(user, 5)); return null; });

        assertEquals(0, count("SELECT count(*) FROM daily_quests WHERE user_id = '" + user + "'"));
    }

    @Test
    void theDayFollowsTheLearnersTimezone() {
        UUID early = UUID.randomUUID();
        UUID late = UUID.randomUUID();
        timezones.save(early, "Pacific/Kiritimati");
        timezones.save(late, "Etc/GMT+12");

        assertNotEquals(inTransaction(() -> quests.view(early)).date(), inTransaction(() -> quests.view(late)).date());
    }

    private static List<QuestionAttemptEvent> exam(UUID user, int count) {
        UUID attempt = UUID.randomUUID();
        List<QuestionAttemptEvent> exam = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            exam.add(new QuestionAttemptEvent(user, UUID.randomUUID(), List.of(), true, null, null, 1000L, null,
                    QuestionEventSource.ASSESSMENT, UUID.randomUUID(), attempt, true, null));
        }
        return exam;
    }

    private <T> T inTransaction(java.util.function.Supplier<T> work) {
        return new TransactionTemplate(transactions).execute(status -> work.get());
    }

    private static List<QuestionAttemptEvent> answers(UUID user, QuestionEventSource source, UUID reference, int count,
                                                      List<UUID> points) {
        List<QuestionAttemptEvent> list = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            list.add(QuestionAttemptEvent.of(user, UUID.randomUUID(), points, index % 2 == 0, 1000L, null, source,
                    reference, true));
        }
        return list;
    }

    private void profile(UUID user, UUID kp, String tier, double mastery) {
        jdbc.update("INSERT INTO kp_mastery_profiles (user_id, knowledge_point_id, mastery, tier, evidence_count, "
                + "last_evidence_at) VALUES (?, ?, ?, ?, 5, now())", user, kp, mastery, tier);
    }

    private int count(String sql) {
        Integer value = jdbc.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
