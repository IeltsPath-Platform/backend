package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.*;
import com.ieltspath.learning.application.usecase.GetLearnerTreeUseCase;
import com.ieltspath.learning.application.usecase.StartFreePracticeUseCase;
import com.ieltspath.learning.application.usecase.SubmitPracticeAttemptUseCase;
import com.ieltspath.learning.domain.vo.LearningSkill;
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

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** Free practice on one knowledge point, end to end against a real database and a stubbed Content. */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "learning.messaging.consumer-enabled=false",
        "learning.mastery.backfill-on-startup=false", "learning.free-practice.reserve=2",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class FreePracticeIntegrationTest {
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

    private static final UUID TOPIC = UUID.randomUUID();
    private static final UUID COURSE = UUID.randomUUID();
    private static final UUID KP = UUID.randomUUID();
    private static final UUID PACKAGE = UUID.randomUUID();
    private static final UUID VERSION = UUID.randomUUID();
    private static final UUID FIRST_QUESTION = UUID.randomUUID();
    private static final UUID SECOND_QUESTION = UUID.randomUUID();

    @Autowired JdbcTemplate jdbc;
    @Autowired StartFreePracticeUseCase start;
    @Autowired SubmitPracticeAttemptUseCase submit;
    @Autowired GetLearnerTreeUseCase treeUseCase;
    @MockitoBean LearningContentClient content;

    @BeforeEach
    void catalog() {
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(TOPIC, "READ", "Reading", 1, null,
                List.of(new KnowledgePoint(KP, "R_FREE", "Free point", "PROCEDURE", "READING", "", true)),
                LearningSkill.READING, true,
                new Course(COURSE, "C60", "Band 6.0", new BigDecimal("6.0"), true, true))));
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3))).thenReturn(Map.of(KP, 5));
        when(content.searchPracticeSets(eq(KP), anyList(), eq(3), eq(5)))
                .thenReturn(List.of(new PracticeSet(PACKAGE, VERSION, "P1", 2, 2)));
        when(content.getPackageVersion(VERSION)).thenReturn(version());
    }

    @Test
    void aFreeSetIsRecordedAsFreeEvidenceWithoutReviewsOrALesson() {
        UUID user = enrolled();
        start.execute(user, KP);
        UUID attempt = jdbc.queryForObject("SELECT id FROM practice_attempts WHERE user_id = ?", UUID.class, user);
        assertEquals("FREE", jdbc.queryForObject("SELECT origin FROM practice_attempts WHERE id = ?", String.class,
                attempt));
        assertNull(jdbc.queryForObject("SELECT lesson_id FROM practice_attempts WHERE id = ?", UUID.class, attempt));

        var result = submit.execute(user, attempt, new SubmitExerciseCommand(UUID.randomUUID(), List.of(
                new SubmitExerciseCommand.Answer(FIRST_QUESTION, "A"),
                new SubmitExerciseCommand.Answer(SECOND_QUESTION, "B"))));

        assertEquals(1, result.correct());
        assertEquals(2, result.total());
        assertEquals(2, count("SELECT count(*) FROM kp_evidence WHERE user_id = ? AND source = 'free_practice'", user));
        assertEquals(2, count("SELECT count(*) FROM question_attempt_events WHERE user_id = ? "
                + "AND source = 'free_practice'", user));
        assertEquals(0, count("SELECT count(*) FROM review_items WHERE user_id = ?", user),
                "a free attempt never raises a review, even when it fails");
    }

    @Test
    void startingAgainReturnsTheOpenAttemptInsteadOfAnotherOne() {
        UUID user = enrolled();
        start.execute(user, KP);
        start.execute(user, KP);

        assertEquals(1, count("SELECT count(*) FROM practice_attempts WHERE user_id = ?", user));
    }

    @Test
    void freePracticeStopsAtTheReserveSoTheLastSetsStayForReviews() {
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3))).thenReturn(Map.of(KP, 2));
        UUID user = enrolled();

        LearningRequestException error = assertThrows(LearningRequestException.class,
                () -> start.execute(user, KP));

        assertEquals(409, error.getStatus());
        assertEquals("FREE_PRACTICE_EXHAUSTED", error.getCode());
        assertEquals(0, count("SELECT count(*) FROM practice_attempts WHERE user_id = ?", user));
    }

    @Test
    void aLearnerWhoIsNotTakingTheCourseCannotStartFreePractice() {
        UUID stranger = UUID.randomUUID();

        LearningRequestException error = assertThrows(LearningRequestException.class,
                () -> start.execute(stranger, KP));

        assertEquals(403, error.getStatus());
        assertEquals("COURSE_NOT_JOINED", error.getCode());
    }

    @Test
    void theTreeListsThePointsOfTheCourseWithFreePracticeOpen() {
        var tree = treeUseCase.execute(enrolled(), COURSE);

        assertEquals(COURSE, tree.courseId());
        assertEquals(1, tree.knowledgePoints().size());
        var node = tree.knowledgePoints().get(0);
        assertEquals("R_FREE", node.code());
        assertNull(node.mastery(), "no evidence yet, so no score or tier");
        assertNull(node.tier());
        assertTrue(node.practiceAvailable());
    }

    @Test
    void theTreeClosesFreePracticeWhenOnlyTheReserveIsLeft() {
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3))).thenReturn(Map.of(KP, 2));

        var node = treeUseCase.execute(enrolled(), COURSE).knowledgePoints().get(0);

        assertFalse(node.practiceAvailable());
    }

    @Test
    void aLearnerWhoIsNotTakingTheCourseGetsNoTree() {
        LearningRequestException error = assertThrows(LearningRequestException.class,
                () -> treeUseCase.execute(UUID.randomUUID(), COURSE));

        assertEquals(403, error.getStatus());
    }

    @Test
    void aPointContentDoesNotKnowIsNotFound() {
        UUID user = enrolled();

        LearningRequestException error = assertThrows(LearningRequestException.class,
                () -> start.execute(user, UUID.randomUUID()));

        assertEquals(404, error.getStatus());
    }

    private UUID enrolled() {
        UUID user = UUID.randomUUID();
        jdbc.update("INSERT INTO topic_progress (user_id, topic_id, sequence_order) VALUES (?, ?, 1)", user, TOPIC);
        return user;
    }

    private int count(String sql, UUID user) {
        return jdbc.queryForObject(sql, Integer.class, user);
    }

    private static PackageVersion version() {
        List<Item> items = List.of(
                new Item(FIRST_QUESTION, 1, "Choose", List.of(new Option("A", "Correct", 1),
                        new Option("B", "Wrong", 2)), Map.of("type", "CHOICE", "correct", "A"), "Because A",
                        BigDecimal.ONE, List.of(new KnowledgePointMapping(KP, BigDecimal.ONE))),
                new Item(SECOND_QUESTION, 2, "Choose", List.of(new Option("A", "Correct", 1),
                        new Option("B", "Wrong", 2)), Map.of("type", "CHOICE", "correct", "A"), "Because A",
                        BigDecimal.ONE, List.of(new KnowledgePointMapping(KP, BigDecimal.ONE))));
        return new PackageVersion(VERSION, PACKAGE, "PRACTICE_SET", null, Map.of(),
                List.of(new Section(UUID.randomUUID(), "Practice", "READING", null, 1, "Passage", items)));
    }
}
