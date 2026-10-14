package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.command.AssessmentResult.ItemResult;
import com.ieltspath.learning.application.command.AssessmentResult.KnowledgePointJudgment;
import com.ieltspath.learning.application.command.AssessmentResult;
import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.port.LearningContentClient.*;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.LessonPracticeSetsResult;
import com.ieltspath.learning.application.result.SubmissionResult;
import com.ieltspath.learning.application.service.ItemGrading;
import com.ieltspath.learning.application.usecase.ApplyAssessmentResultUseCase;
import com.ieltspath.learning.application.usecase.AssignTopicTestUseCase;
import com.ieltspath.learning.application.usecase.GetLessonPracticeSetsUseCase;
import com.ieltspath.learning.application.usecase.GetLessonUseCase;
import com.ieltspath.learning.application.usecase.RefreshLearningTopicsUseCase;
import com.ieltspath.learning.application.usecase.StartPracticeAttemptUseCase;
import com.ieltspath.learning.application.usecase.CompleteLessonUseCase;
import com.ieltspath.learning.application.usecase.SubmitPracticeAttemptUseCase;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.domain.exception.LearningGateException;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PracticePassReason;
import com.ieltspath.learning.domain.vo.PracticeStatus;
import com.ieltspath.learning.domain.vo.PracticeSubmission;
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
    private static final Set<LearningSkill> THREE = Set.of(LearningSkill.LISTENING, LearningSkill.READING,
            LearningSkill.WRITING);
    private static final UUID READING_SET = UUID.randomUUID();
    private static final UUID LISTENING_SET = UUID.randomUUID();
    private static final UUID MIXED_SET = UUID.randomUUID();
    private static final UUID MIXED_VERSION = UUID.randomUUID();
    private static final UUID READING_VERSION = UUID.randomUUID();
    private static final UUID LISTENING_VERSION = UUID.randomUUID();
    private static final UUID WRITING_SET = UUID.randomUUID();

    @Autowired JdbcTemplate jdbc;
    @Autowired RefreshLearningTopicsUseCase topics;
    @Autowired GetLessonUseCase getLesson;
    @Autowired CompleteLessonUseCase completeLesson;
    @Autowired AssignTopicTestUseCase assignTopicTest;
    @Autowired ApplyAssessmentResultUseCase applyResult;
    @Autowired GetLessonPracticeSetsUseCase practiceSets;
    @Autowired StartPracticeAttemptUseCase startPractice;
    @Autowired SubmitPracticeAttemptUseCase submitPractice;
    @MockitoBean LearningContentClient content;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE review_theory_checks, review_sets, review_items, practice_attempts, "
                + "lesson_practice_passes, kp_evidence, lesson_progress, topic_progress, "
                + "knowledge_point_catalog, topic_test_assignments, assessment_result_versions CASCADE");
        Course course = new Course(UUID.randomUUID(), "IELTS_5_5", "IELTS 5.5", new BigDecimal("5.5"), false);
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(TOPIC, "TREES", "Trees", 955, null, List.of(
                new KnowledgePoint(KP_READING, "TR_R", "Scanning", "PROCEDURE", "READING", "d", true),
                new KnowledgePoint(KP_LISTENING, "TR_L", "Details", "PROCEDURE", "LISTENING", "d", true),
                new KnowledgePoint(KP_WRITING, "TR_W", "Opinion", "PROCEDURE", "WRITING", "d", false)),
                null, true, course, THREE)));
        when(content.getTopicLessons(TOPIC)).thenReturn(List.of(
                new LessonSummary(MIXED, TOPIC, "T1", "Trees", null, 1,
                        List.of(KP_READING, KP_LISTENING, KP_WRITING), THREE),
                new LessonSummary(LISTENING_ONLY, TOPIC, "T2", "More listening", null, 2, List.of(KP_LISTENING),
                        Set.of(LearningSkill.LISTENING))));
        when(content.getLesson(MIXED)).thenReturn(new Lesson(MIXED, TOPIC, "T1", "Trees", null,
                "Scan for the detail.", 1, List.of(KP_READING, KP_LISTENING, KP_WRITING), null, THREE));
        when(content.getLesson(LISTENING_ONLY)).thenReturn(new Lesson(LISTENING_ONLY, TOPIC, "T2", "More listening",
                null, null, 2, List.of(KP_LISTENING), LearningSkill.LISTENING, Set.of(LearningSkill.LISTENING)));
        when(content.getTopicTestPackages(TOPIC)).thenReturn(List.of(
                new TestPackage(UUID.randomUUID(), UUID.randomUUID(), "TR-TOPIC-TEST")));
        List<LessonPracticeSet> sets = List.of(
                practiceSet(READING_SET, READING_VERSION, "TR-R", Set.of(LearningSkill.READING)),
                practiceSet(LISTENING_SET, LISTENING_VERSION, "TR-L", Set.of(LearningSkill.LISTENING)),
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
        when(content.getPackageVersion(READING_VERSION)).thenReturn(new PackageVersion(READING_VERSION, READING_SET,
                "PRACTICE_SET", null, Map.of(), List.of(
                new Section(UUID.randomUUID(), "Roots", "READING", null, 1, "Passage", items(KP_READING, 2)))));
        when(content.getPackageVersion(LISTENING_VERSION)).thenReturn(new PackageVersion(LISTENING_VERSION,
                LISTENING_SET, "PRACTICE_SET", null, Map.of(), List.of(
                new Section(UUID.randomUUID(), "Gym", "LISTENING", null, 1, null, items(KP_LISTENING, 3)))));
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3)))
                .thenReturn(Map.of(KP_READING, 1, KP_LISTENING, 1));
        topics.execute(USER);
    }

    @Test
    void aMixedSetIsScoredPerSkillAndOnlyTheFailedSkillGoesToReview() {
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
    void everyRequiredSetMustBeSubmittedAndTheWritingSetIsOptional() {
        var view = startPractice.execute(USER, MIXED, MIXED_SET);
        assertTrue(submitPractice.execute(USER, view.attemptId(), answers("A", "A", "A", "A", "A")).passed());
        // One set of three is not enough, even when it passed both of its skills.
        assertEquals(PracticeStatus.REQUIRED, practiceSets.execute(USER, MIXED).practiceStatus());
        assertEquals("AVAILABLE", getLesson.execute(USER, MIXED).status());

        submitAllCorrect(READING_SET, READING_VERSION);
        assertEquals(PracticeStatus.REQUIRED, practiceSets.execute(USER, MIXED).practiceStatus());
        submitAllCorrect(LISTENING_SET, LISTENING_VERSION);

        var catalog = practiceSets.execute(USER, MIXED);
        assertEquals(PracticeStatus.PASSED, catalog.practiceStatus());
        assertEquals(PracticePassReason.FIRST_SUBMISSION, catalog.practicePassReason());
        assertEquals("COMPLETED", getLesson.execute(USER, MIXED).status());
    }

    @Test
    void theMixedLessonShowsItsSkillsAndCompletesWhenItsPracticePasses() {
        var before = getLesson.execute(USER, MIXED);
        assertEquals(THREE, before.skills());
        assertEquals("AVAILABLE", before.status());

        var refused = assertThrows(LearningRequestException.class, () -> completeLesson.execute(USER, MIXED));
        assertEquals("PRACTICE_REQUIRED", refused.getCode());
        assertEquals(List.of(MIXED), refused.getLessonIds());
        assertEquals("AVAILABLE", getLesson.execute(USER, MIXED).status());

        passEveryRequiredSet();

        assertEquals("COMPLETED", getLesson.execute(USER, MIXED).status());
    }

    @Test
    void aPendingReviewHoldsBackTheOtherLessonsOfItsSkillAndTheTestButNotTheLessonThatCreatedIt() {
        passEveryRequiredSet();
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill) "
                + "VALUES (?, ?, ?, ?, 'PENDING', 'READING')", UUID.randomUUID(), USER, KP_READING, MIXED);

        // The lesson that created the review stays open to read, but its Practice waits for the review.
        assertDoesNotThrow(() -> getLesson.execute(USER, MIXED));
        assertEquals("REVIEW_REQUIRED", assertThrows(LearningGateException.class,
                () -> startPractice.execute(USER, MIXED, READING_SET)).getCode());
        // A Reading review does not hold back a lesson that only teaches Listening; the final test waits.
        assertDoesNotThrow(() -> getLesson.execute(USER, LISTENING_ONLY));
        assertEquals("REVIEW_REQUIRED", assertThrows(LearningGateException.class,
                () -> assignTopicTest.execute(USER, TOPIC)).getCode());

        // A Listening review of the first lesson holds back the next lesson, which teaches Listening.
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill) "
                + "VALUES (?, ?, ?, ?, 'PENDING', 'LISTENING')", UUID.randomUUID(), USER, KP_LISTENING, MIXED);
        assertEquals("REVIEW_REQUIRED", assertThrows(LearningGateException.class,
                () -> getLesson.execute(USER, LISTENING_ONLY)).getCode());
    }

    @Test
    void aMissedReadingKnowledgePointInATestCreatesAReadingReview() {
        markMixedLessonCompleted();
        applyResult.execute(new AssessmentResult(UUID.randomUUID(), USER, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), 1, "TOPIC_GATE", Instant.now(), List.of(new ItemResult(UUID.randomUUID(),
                UUID.randomUUID(), false, BigDecimal.ZERO, BigDecimal.ONE,
                List.of(new KnowledgePointJudgment(KP_READING, null))))));

        assertEquals("READING", jdbc.queryForObject("SELECT skill FROM review_items WHERE user_id = ? "
                + "AND knowledge_point_id = ? AND status = 'PENDING'", String.class, USER, KP_READING));
    }

    /** A completed lesson with no Practice evidence, so a later test result can pull its mastery below the threshold. */
    private void markMixedLessonCompleted() {
        jdbc.update("INSERT INTO lesson_progress (user_id, lesson_id, topic_id, lesson_sort_order, knowledge_point_ids, "
                + "completed_at) VALUES (?, ?, ?, 1, ARRAY[?::uuid, ?::uuid], clock_timestamp())",
                USER, MIXED, TOPIC, KP_READING, KP_LISTENING);
    }

    /** Passing every required set clears the Practice, which completes the lesson. */
    private void passEveryRequiredSet() {
        submitAllCorrect(READING_SET, READING_VERSION);
        submitAllCorrect(LISTENING_SET, LISTENING_VERSION);
        submitAllCorrect(MIXED_SET, MIXED_VERSION);
    }

    private void submitAllCorrect(UUID packageId, UUID versionId) {
        var view = startPractice.execute(USER, MIXED, packageId);
        var answers = ItemGrading.orderedItems(content.getPackageVersion(versionId)).stream()
                .map(item -> new SubmitExerciseCommand.Answer(item.questionVersionId(), "A")).toList();
        assertTrue(submitPractice.execute(USER, view.attemptId(),
                new SubmitExerciseCommand(UUID.randomUUID(), answers)).passed());
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
}
