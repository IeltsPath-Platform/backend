package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
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
import static org.mockito.ArgumentMatchers.anyInt;
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
    private static final UUID TRAP_KP = UUID.randomUUID();
    private static final UUID PACKAGE = UUID.randomUUID();
    private static final UUID VERSION = UUID.randomUUID();
    private static final UUID FIRST_QUESTION = UUID.randomUUID();
    private static final UUID SECOND_QUESTION = UUID.randomUUID();
    private static final UUID THIRD_QUESTION = UUID.randomUUID();

    @Autowired JdbcTemplate jdbc;
    @Autowired com.ieltspath.learning.application.usecase.RefreshLearningTopicsUseCase topics;
    @Autowired StartFreePracticeUseCase start;
    @Autowired SubmitPracticeAttemptUseCase submit;
    @Autowired GetLearnerTreeUseCase treeUseCase;
    @Autowired com.ieltspath.learning.application.port.CourseScoreData courseScoreData;
    @MockitoBean LearningContentClient content;
    @MockitoBean AccessClient access;

    @BeforeEach
    void catalog() {
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(TOPIC, "READ", "Reading", 1, null,
                List.of(new KnowledgePoint(KP, "R_FREE", "Free point", "PROCEDURE", "READING", "", true)),
                LearningSkill.READING, true,
                new Course(COURSE, "C60", "Band 6.0", new BigDecimal("6.0"), true, true))));
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3))).thenReturn(Map.of(KP, 5));
        when(content.searchPracticeSets(eq(KP), anyList(), eq(3), anyInt()))
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
    void countedFreePracticeSubmissionPersistsTaggedChildEvidence() {
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(TOPIC, "READ", "Reading", 1, null,
                List.of(new KnowledgePoint(KP, "R_FREE", "Free point", "PROCEDURE", "READING", "", true),
                        new KnowledgePoint(TRAP_KP, "R_TRAP", "Trap", "PROCEDURE", "READING", "", false,
                                null, null, KP, "TFNG_NG_AS_FALSE")), LearningSkill.READING, true,
                new Course(COURSE, "C60", "Band 6.0", new BigDecimal("6.0"), true, true))));
        when(content.getPackageVersion(VERSION)).thenReturn(taggedVersion());
        UUID user = enrolled();
        topics.execute(user);
        start.execute(user, KP);
        UUID attempt = jdbc.queryForObject("SELECT id FROM practice_attempts WHERE user_id = ?", UUID.class, user);

        var submission = submit.execute(user, attempt, new SubmitExerciseCommand(UUID.randomUUID(), List.of(
                new SubmitExerciseCommand.Answer(FIRST_QUESTION, "B"),
                new SubmitExerciseCommand.Answer(SECOND_QUESTION, "A"))));

        assertTrue(submission.countedAsEvidence());
        assertEquals(1, count("SELECT count(*) FROM knowledge_point_catalog WHERE kp_id = ? "
                + "AND parent_kp_id = ? AND error_code = 'TFNG_NG_AS_FALSE'", TRAP_KP, KP));
        assertEquals(1, count("SELECT count(*) FROM kp_evidence WHERE user_id = ? AND kp_id = ? "
                + "AND source = 'error_trap' AND correct = false", user, TRAP_KP));
        var parentNode = treeUseCase.execute(user, COURSE).knowledgePoints().stream()
                .filter(node -> node.knowledgePointId().equals(KP)).findFirst().orElseThrow();
        assertEquals(List.of(TRAP_KP), parentNode.errorTraps().stream()
                .map(trap -> trap.knowledgePointId()).toList());
        assertEquals(List.of("TFNG_NG_AS_FALSE"), parentNode.errorTraps().stream()
                .map(trap -> trap.code()).toList(), "the tree names the error code, not the child point's own code");
    }

    @Test
    void freePracticeSubmissionCreatesAnErrorPatternForItsCompletedParentLesson() {
        UUID lessonId = UUID.nameUUIDFromBytes("parent-lesson".getBytes());
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(TOPIC, "READ", "Reading", 1, null,
                List.of(new KnowledgePoint(KP, "R_FREE", "Free point", "PROCEDURE", "READING", "", true),
                        new KnowledgePoint(TRAP_KP, "R_TRAP", "Trap", "PROCEDURE", "READING", "", false,
                                null, null, KP, "TFNG_NG_AS_FALSE")), LearningSkill.READING, true,
                new Course(COURSE, "C60", "Band 6.0", new BigDecimal("6.0"), true, true))));
        when(content.getLesson(lessonId)).thenReturn(
                new Lesson(lessonId, TOPIC, "L1", "Parent lesson", null,
                        1, List.of(KP), List.of(), LearningSkill.READING));
        when(content.getPackageVersion(VERSION)).thenReturn(taggedWrongVersion());
        when(content.searchPracticeSets(eq(KP), anyList(), eq(3), eq(1), eq(lessonId), eq("TFNG_NG_AS_FALSE")))
                .thenReturn(List.of(new PracticeSet(UUID.randomUUID(), UUID.randomUUID(), "TRAP_SET", 3, 3,
                        null, null, 2)));
        UUID user = enrolled();
        topics.execute(user);
        jdbc.update("INSERT INTO lesson_progress (user_id, lesson_id, topic_id, lesson_sort_order, "
                + "knowledge_point_ids, completed_at) VALUES (?, ?, ?, 1, ARRAY[CAST(? AS uuid)], now())",
                user, lessonId, TOPIC, KP.toString());
        jdbc.update("INSERT INTO kp_mastery_profiles (user_id, knowledge_point_id, mastery, tier, evidence_count, "
                + "success_streak, last_evidence_at) VALUES (?, ?, 0.85, 'MAINTAIN', 10, 4, now())", user, KP);
        start.execute(user, KP);
        UUID attempt = jdbc.queryForObject("SELECT id FROM practice_attempts WHERE user_id = ?", UUID.class, user);

        submit.execute(user, attempt, new SubmitExerciseCommand(UUID.randomUUID(), List.of(
                new SubmitExerciseCommand.Answer(FIRST_QUESTION, "B"),
                new SubmitExerciseCommand.Answer(SECOND_QUESTION, "B"),
                new SubmitExerciseCommand.Answer(THIRD_QUESTION, "B"))));

        assertEquals(3, count("SELECT count(*) FROM kp_evidence WHERE user_id = ? AND kp_id = ? "
                + "AND source = 'error_trap' AND correct = false", user, TRAP_KP));
        assertEquals("PRIORITY", jdbc.queryForObject("SELECT tier FROM kp_mastery_profiles WHERE user_id = ? "
                + "AND knowledge_point_id = ?", String.class, user, TRAP_KP));
        assertEquals(1, count("SELECT count(*) FROM review_items WHERE user_id = ? AND knowledge_point_id = ? "
                + "AND trigger_kind = 'ERROR_PATTERN' AND status = 'PENDING' AND blocking = false", user, KP));
    }

    private void offering(BigDecimal band, String featureKey) {
        when(content.searchPracticeSets(eq(KP), anyList(), eq(3), anyInt()))
                .thenReturn(List.of(new PracticeSet(PACKAGE, VERSION, "P1", 2, 2, band, featureKey)));
    }

    @Test
    void aSetAboveTheBandOfTheCourseIsNotOffered() {
        offering(new BigDecimal("6.5"), null);

        LearningRequestException error = assertThrows(LearningRequestException.class,
                () -> start.execute(enrolled(), KP));

        assertEquals("FREE_PRACTICE_EXHAUSTED", error.getCode());
    }

    @Test
    void aSetAtOrBelowTheBandOfTheCourseIsOffered() {
        offering(new BigDecimal("6.0"), null);
        UUID user = enrolled();

        start.execute(user, KP);

        assertEquals(1, count("SELECT count(*) FROM practice_attempts WHERE user_id = ?", user));
    }

    @Test
    void aSetThatNeedsAPlanIsHeldBackFromALearnerWithoutOne() {
        offering(null, "PREMIUM_CONTENT");
        when(access.premium()).thenReturn(false);

        LearningRequestException error = assertThrows(LearningRequestException.class,
                () -> start.execute(enrolled(), KP));

        assertEquals("FREE_PRACTICE_EXHAUSTED", error.getCode());
    }

    @Test
    void aSetThatNeedsAPlanIsOfferedToASubscriber() {
        offering(null, "PREMIUM_CONTENT");
        when(access.premium()).thenReturn(true);
        UUID user = enrolled();

        start.execute(user, KP);

        assertEquals(1, count("SELECT count(*) FROM practice_attempts WHERE user_id = ?", user));
    }

    @Test
    void whenAccessCannotAnswerTheLearnerCountsAsHavingNoPlan() {
        offering(null, "PREMIUM_CONTENT");
        when(access.premium()).thenThrow(new AccessUnavailableException(503));

        LearningRequestException error = assertThrows(LearningRequestException.class,
                () -> start.execute(enrolled(), KP));

        assertEquals("FREE_PRACTICE_EXHAUSTED", error.getCode());
    }

    @Test
    void aFreeSetNeverEntersTheProcessScoreOfTheCourse() {
        UUID user = enrolled();
        jdbc.update("UPDATE topic_progress SET course_id = ? WHERE user_id = ?", COURSE, user);
        start.execute(user, KP);
        UUID attempt = jdbc.queryForObject("SELECT id FROM practice_attempts WHERE user_id = ?", UUID.class, user);

        submit.execute(user, attempt, new SubmitExerciseCommand(UUID.randomUUID(), List.of(
                new SubmitExerciseCommand.Answer(FIRST_QUESTION, "A"),
                new SubmitExerciseCommand.Answer(SECOND_QUESTION, "A"))));

        assertEquals(1, count("SELECT count(*) FROM practice_attempts WHERE user_id = ? AND submitted_at IS NOT NULL",
                user), "the free set was submitted");
        assertTrue(courseScoreData.practiceAverage(user, COURSE).isEmpty(),
                "a free set has no lesson, so the Practice average of the course ignores it");
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
    void theTreeShowsTheTierOfAPointTheLearnerHasEvidenceOn() {
        UUID user = enrolled();
        jdbc.update("INSERT INTO kp_mastery_profiles (user_id, knowledge_point_id, mastery, tier, evidence_count, "
                + "last_evidence_at) VALUES (?, ?, 0.7, 'CONSOLIDATE', 6, now())", user, KP);

        var node = treeUseCase.execute(user, COURSE).knowledgePoints().get(0);

        assertEquals("CONSOLIDATE", node.tier());
        assertEquals(0.7, node.mastery(), 1e-9);
        assertEquals(6, node.evidenceCount());
    }

    @Test
    void theTreeCarriesTheCefrLevelAndCanDoContentGives() {
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(TOPIC, "READ", "Reading", 1, null,
                List.of(new KnowledgePoint(KP, "R_FREE", "Free point", "PROCEDURE", "READING", "", true, "B1",
                        "Find the main idea of a short text")),
                LearningSkill.READING, true,
                new Course(COURSE, "C60", "Band 6.0", new BigDecimal("6.0"), true, true))));

        var node = treeUseCase.execute(enrolled(), COURSE).knowledgePoints().get(0);

        assertEquals("B1", node.cefrLevel());
        assertEquals("Find the main idea of a short text", node.canDo());
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

    private int count(String sql, UUID... args) {
        return jdbc.queryForObject(sql, Integer.class, (Object[]) args);
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

    private static PackageVersion taggedVersion() {
        List<Item> items = List.of(
                new Item(FIRST_QUESTION, 1, "Choose", List.of(new Option("A", "Correct", 1),
                        new Option("B", "Wrong", 2)), Map.of("type", "CHOICE", "correct", "A",
                        "distractors", Map.of("B", "TFNG_NG_AS_FALSE")), "Because A", BigDecimal.ONE,
                        List.of(new KnowledgePointMapping(KP, BigDecimal.ONE))),
                new Item(SECOND_QUESTION, 2, "Choose", List.of(new Option("A", "Correct", 1),
                        new Option("B", "Wrong", 2)), Map.of("type", "CHOICE", "correct", "A"), "Because A",
                        BigDecimal.ONE, List.of(new KnowledgePointMapping(KP, BigDecimal.ONE))));
        return new PackageVersion(VERSION, PACKAGE, "PRACTICE_SET", null, Map.of(),
                List.of(new Section(UUID.randomUUID(), "Practice", "READING", null, 1, "Passage", items)));
    }

    private static PackageVersion taggedWrongVersion() {
        List<Item> items = List.of(FIRST_QUESTION, SECOND_QUESTION, THIRD_QUESTION).stream().map(id ->
                new Item(id, 1, "Choose", List.of(new Option("A", "Correct", 1), new Option("B", "Wrong", 2)),
                        Map.of("type", "CHOICE", "correct", "A", "distractors", Map.of("B", "TFNG_NG_AS_FALSE")),
                        "Because A", BigDecimal.ONE, List.of(new KnowledgePointMapping(KP, BigDecimal.ONE)))).toList();
        return new PackageVersion(VERSION, PACKAGE, "PRACTICE_SET", null, Map.of(),
                List.of(new Section(UUID.randomUUID(), "Practice", "READING", null, 1, "Passage", items)));
    }
}
