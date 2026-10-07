package com.group01.learning.infrastructure.persistence;

import com.group01.learning.application.command.AssessmentResult.ItemResult;
import com.group01.learning.application.command.AssessmentResult.KnowledgePointJudgment;
import com.group01.learning.application.command.AssessmentResult;
import com.group01.learning.application.command.SubmitExerciseCommand;
import com.group01.learning.application.port.LearningContentClient.*;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.result.LessonPracticeSetsResult;
import com.group01.learning.application.result.SubmissionResult;
import com.group01.learning.application.usecase.ApplyAssessmentResultUseCase;
import com.group01.learning.application.usecase.AssignTopicTestUseCase;
import com.group01.learning.application.usecase.GetLessonPracticeSetsUseCase;
import com.group01.learning.application.usecase.GetLessonUseCase;
import com.group01.learning.application.usecase.RefreshLearningTopicsUseCase;
import com.group01.learning.application.usecase.StartPracticeAttemptUseCase;
import com.group01.learning.application.usecase.SubmitLessonExerciseUseCase;
import com.group01.learning.application.usecase.SubmitPracticeAttemptUseCase;
import com.group01.learning.domain.exception.LearningGateException;
import com.group01.learning.domain.vo.LearningSkill;
import com.group01.learning.domain.vo.PracticePassReason;
import com.group01.learning.domain.vo.PracticeStatus;
import com.group01.learning.domain.vo.PracticeSubmission;
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
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** A topic whose first lesson teaches Reading, Listening and Writing, and whose second lesson teaches Listening only. */
@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false",
        "learning.messaging.consumer-enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class MultiSkillLessonIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db").withPassword(UUID.randomUUID().toString());

    private static final byte[] INTERNAL_KEY = new byte[32];
    static { new SecureRandom().nextBytes(INTERNAL_KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    private static final UUID USER = UUID.randomUUID();
    private static final UUID TOPIC = UUID.randomUUID();
    private static final UUID MIXED = UUID.randomUUID();
    private static final UUID LISTENING_ONLY = UUID.randomUUID();
    private static final UUID KP_READING = UUID.randomUUID();
    private static final UUID KP_LISTENING = UUID.randomUUID();
    private static final UUID KP_WRITING = UUID.randomUUID();
    private static final UUID READING_BLOCK = UUID.randomUUID();
    private static final UUID LISTENING_BLOCK = UUID.randomUUID();
    private static final UUID ESSAY_BLOCK = UUID.randomUUID();
    private static final UUID SECOND_BLOCK = UUID.randomUUID();
    private static final UUID READING_QUESTION = UUID.randomUUID();
    private static final UUID LISTENING_QUESTION = UUID.randomUUID();
    private static final UUID SECOND_QUESTION = UUID.randomUUID();
    private static final Set<LearningSkill> THREE = Set.of(LearningSkill.LISTENING, LearningSkill.READING,
            LearningSkill.WRITING);
    private static final UUID READING_SET = UUID.randomUUID();
    private static final UUID LISTENING_SET = UUID.randomUUID();
    private static final UUID MIXED_SET = UUID.randomUUID();
    private static final UUID MIXED_VERSION = UUID.randomUUID();
    private static final UUID WRITING_SET = UUID.randomUUID();

    @Autowired JdbcTemplate jdbc;
    @Autowired RefreshLearningTopicsUseCase topics;
    @Autowired GetLessonUseCase getLesson;
    @Autowired SubmitLessonExerciseUseCase submitExercise;
    @Autowired AssignTopicTestUseCase assignTopicTest;
    @Autowired ApplyAssessmentResultUseCase applyResult;
    @Autowired GetLessonPracticeSetsUseCase practiceSets;
    @Autowired StartPracticeAttemptUseCase startPractice;
    @Autowired SubmitPracticeAttemptUseCase submitPractice;
    @MockitoBean LearningContentClient content;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE review_theory_checks, review_sets, review_items, practice_attempts, "
                + "lesson_practice_passes, lesson_exercise_submissions, kp_evidence, lesson_progress, topic_progress, "
                + "knowledge_point_catalog, topic_test_assignments, assessment_result_versions CASCADE");
        Course course = new Course(UUID.randomUUID(), "IELTS_5_5", "IELTS 5.5", new BigDecimal("5.5"), false);
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(TOPIC, "TREES", "Trees", 955, null, List.of(
                new KnowledgePoint(KP_READING, "TR_R", "Scanning", "PROCEDURE", "READING", "d", true),
                new KnowledgePoint(KP_LISTENING, "TR_L", "Details", "PROCEDURE", "LISTENING", "d", true),
                new KnowledgePoint(KP_WRITING, "TR_W", "Opinion", "PROCEDURE", "WRITING", "d", false)),
                null, true, course, THREE)));
        when(content.getTopicLessons(TOPIC)).thenReturn(List.of(
                new LessonSummary(MIXED, TOPIC, "T1", "Trees", null, 1,
                        List.of(KP_READING, KP_LISTENING, KP_WRITING), List.of(READING_BLOCK, LISTENING_BLOCK, ESSAY_BLOCK),
                        THREE),
                new LessonSummary(LISTENING_ONLY, TOPIC, "T2", "More listening", null, 2, List.of(KP_LISTENING),
                        List.of(SECOND_BLOCK), Set.of(LearningSkill.LISTENING))));
        Question essay = new Question(UUID.randomUUID(), 1, "Do you agree?", null,
                Map.of("type", "ESSAY", "task", "TASK_2", "minWords", 250, "passBand", 5.5), "Model", List.of(KP_WRITING));
        when(content.getLesson(MIXED)).thenReturn(new Lesson(MIXED, TOPIC, "T1", "Trees", null, 1,
                List.of(KP_READING, KP_LISTENING, KP_WRITING), List.of(
                new Block(UUID.randomUUID(), "TEXT", 1, "Scan for the detail.", null, null, null),
                new Block(READING_BLOCK, "EXERCISE", 2, null, null, null, List.of(choice(READING_QUESTION, KP_READING)),
                        "EXERCISE"),
                new Block(LISTENING_BLOCK, "EXERCISE", 3, null, null, null,
                        List.of(choice(LISTENING_QUESTION, KP_LISTENING)), "EXERCISE"),
                new Block(ESSAY_BLOCK, "EXERCISE", 4, null, null, null, List.of(essay), "ESSAY")), null, THREE));
        when(content.getLesson(LISTENING_ONLY)).thenReturn(new Lesson(LISTENING_ONLY, TOPIC, "T2", "More listening",
                null, 2, List.of(KP_LISTENING), List.of(new Block(SECOND_BLOCK, "EXERCISE", 1, null, null, null,
                List.of(choice(SECOND_QUESTION, KP_LISTENING)), "EXERCISE")), LearningSkill.LISTENING,
                Set.of(LearningSkill.LISTENING)));
        when(content.getTopicTestPackages(TOPIC)).thenReturn(List.of(
                new TestPackage(UUID.randomUUID(), UUID.randomUUID(), "TR-TOPIC-TEST")));
        List<LessonPracticeSet> sets = List.of(
                practiceSet(READING_SET, UUID.randomUUID(), "TR-R", Set.of(LearningSkill.READING)),
                practiceSet(LISTENING_SET, UUID.randomUUID(), "TR-L", Set.of(LearningSkill.LISTENING)),
                practiceSet(MIXED_SET, MIXED_VERSION, "TR-RL", Set.of(LearningSkill.READING, LearningSkill.LISTENING)),
                practiceSet(WRITING_SET, UUID.randomUUID(), "TR-W", Set.of(LearningSkill.WRITING)));
        when(content.lessonPracticeSets(MIXED)).thenReturn(sets);
        when(content.lessonPracticeSets(LISTENING_ONLY)).thenReturn(List.of());
        when(content.topicPracticeSets(TOPIC)).thenReturn(new TopicPracticeSets(List.of(
                new TopicPracticeSets.LessonSets(MIXED, sets), new TopicPracticeSets.LessonSets(LISTENING_ONLY, List.of()))));
        when(content.getPackageVersion(MIXED_VERSION)).thenReturn(new PackageVersion(MIXED_VERSION, MIXED_SET,
                "PRACTICE_SET", null, Map.of(), List.of(
                new Section(UUID.randomUUID(), "Roots", "READING", null, 1, "Passage", items(KP_READING, 2)),
                new Section(UUID.randomUUID(), "Gym", "LISTENING", null, 2, null, items(KP_LISTENING, 3)))));
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3)))
                .thenReturn(Map.of(KP_READING, 1, KP_LISTENING, 1));
        topics.execute(USER);
    }

    @Test
    void aMixedSetIsScoredPerSkillAndOnlyTheFailedSkillGoesToReview() {
        completeMixedLesson("A");
        assertEquals(PracticeStatus.REQUIRED, practiceSets.execute(USER, MIXED).practiceStatus());
        assertEquals(List.of("TR-R"), practiceSets.execute(USER, MIXED, Optional.of(LearningSkill.READING)).items()
                .stream().map(LessonPracticeSetsResult.Item::code).toList());

        var view = startPractice.execute(USER, MIXED, MIXED_SET);
        var result = submitPractice.execute(USER, view.attemptId(), answers("A", "A", "A", "B", "B"));

        assertFalse(result.passed());
        assertEquals(List.of(LearningSkill.LISTENING, LearningSkill.READING),
                result.skillScores().stream().map(PracticeSubmission.SkillScore::skill).toList());
        assertFalse(result.skillScores().get(0).passed());
        assertTrue(result.skillScores().get(1).passed());
        assertEquals(List.of(KP_LISTENING), result.reviewsCreated().stream()
                .map(PracticeSubmission.ReviewCreated::knowledgePointId).toList());
        assertEquals("LISTENING", jdbc.queryForObject("SELECT skill FROM review_items WHERE user_id = ? "
                + "AND knowledge_point_id = ?", String.class, USER, KP_LISTENING));
        assertEquals(PracticeStatus.REQUIRED, practiceSets.execute(USER, MIXED).practiceStatus());
    }

    @Test
    void passingBothPartsOfTheMixedSetClearsThePracticeWithoutTheWritingSet() {
        completeMixedLesson("A");
        var view = startPractice.execute(USER, MIXED, MIXED_SET);
        assertTrue(submitPractice.execute(USER, view.attemptId(), answers("A", "A", "A", "A", "A")).passed());

        var catalog = practiceSets.execute(USER, MIXED);
        assertEquals(PracticeStatus.PASSED, catalog.practiceStatus());
        assertEquals(PracticePassReason.FIRST_SUBMISSION, catalog.practicePassReason());
    }

    @Test
    void theMixedLessonCompletesOnceReadingAndListeningPassWithoutTheEssay() {
        assertEquals(THREE, getLesson.execute(USER, MIXED).skills());
        assertFalse(answer(MIXED, READING_BLOCK, READING_QUESTION, "A").lessonCompleted());
        assertTrue(answer(MIXED, LISTENING_BLOCK, LISTENING_QUESTION, "A").lessonCompleted());
    }

    @Test
    void aReadingReviewLocksLessonsAndTheTestThatTeachReadingButNotAListeningOnlyLesson() {
        completeMixedLesson("A");
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill) "
                + "VALUES (?, ?, ?, ?, 'PENDING', 'READING')", UUID.randomUUID(), USER, KP_READING, MIXED);

        assertEquals("REVIEW_REQUIRED", assertThrows(LearningGateException.class,
                () -> getLesson.execute(USER, MIXED)).getCode());
        assertDoesNotThrow(() -> getLesson.execute(USER, LISTENING_ONLY));
        assertEquals("REVIEW_REQUIRED", assertThrows(LearningGateException.class,
                () -> assignTopicTest.execute(USER, TOPIC)).getCode());
    }

    @Test
    void aMissedReadingKnowledgePointInATestCreatesAReadingReview() {
        completeMixedLesson("B");
        applyResult.execute(new AssessmentResult(UUID.randomUUID(), USER, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), 1, "TOPIC_GATE", Instant.now(), List.of(new ItemResult(UUID.randomUUID(),
                UUID.randomUUID(), false, BigDecimal.ZERO, BigDecimal.ONE,
                List.of(new KnowledgePointJudgment(KP_READING, null))))));

        assertEquals("READING", jdbc.queryForObject("SELECT skill FROM review_items WHERE user_id = ? "
                + "AND knowledge_point_id = ? AND status = 'PENDING'", String.class, USER, KP_READING));
    }

    /** {@code firstReading} is the first Reading answer; a wrong one leaves weak evidence before the retry. */
    private void completeMixedLesson(String firstReading) {
        answer(MIXED, READING_BLOCK, READING_QUESTION, firstReading);
        if (!"A".equals(firstReading)) answer(MIXED, READING_BLOCK, READING_QUESTION, "A");
        assertTrue(answer(MIXED, LISTENING_BLOCK, LISTENING_QUESTION, "A").lessonCompleted());
    }

    private SubmissionResult answer(UUID lesson, UUID block, UUID question, String value) {
        return submitExercise.execute(USER, lesson, block, new SubmitExerciseCommand(UUID.randomUUID(),
                List.of(new SubmitExerciseCommand.Answer(question, value))));
    }

    private static LessonPracticeSet practiceSet(UUID id, UUID version, String code, Set<LearningSkill> skills) {
        return new LessonPracticeSet(id, version, code, code, 3, List.of(), null, skills);
    }

    /** Items of one section; the answers below follow section order, then item order. */
    private static final List<UUID> ITEM_IDS = java.util.stream.Stream.generate(UUID::randomUUID).limit(5).toList();

    private static List<Item> items(UUID kp, int count) {
        int offset = kp.equals(KP_READING) ? 0 : 2;
        return java.util.stream.IntStream.range(0, count).mapToObj(index -> new Item(ITEM_IDS.get(offset + index),
                index + 1, "Pick", List.of(new Option("A", "Right", 1), new Option("B", "Wrong", 2)),
                Map.of("type", "CHOICE", "correct", "A"), "Because", BigDecimal.ONE,
                List.of(new KnowledgePointMapping(kp, BigDecimal.ONE)))).toList();
    }

    private static SubmitExerciseCommand answers(String... values) {
        return new SubmitExerciseCommand(UUID.randomUUID(), java.util.stream.IntStream.range(0, values.length)
                .mapToObj(index -> new SubmitExerciseCommand.Answer(ITEM_IDS.get(index), values[index])).toList());
    }

    private static Question choice(UUID id, UUID kp) {
        return new Question(id, 1, "Which one?", List.of(new Option("A", "Right", 1), new Option("B", "Wrong", 2),
                new Option("C", "Also wrong", 3)), Map.of("type", "CHOICE", "correct", "A"), "Because", List.of(kp));
    }
}
