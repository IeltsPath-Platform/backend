package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.exception.InsufficientPointsException;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.*;
import com.ieltspath.learning.application.port.LlmClient;
import com.ieltspath.learning.application.usecase.GetLessonPracticeSetsUseCase;
import com.ieltspath.learning.application.usecase.GetPracticeAttemptUseCase;
import com.ieltspath.learning.application.usecase.RefreshLearningTopicsUseCase;
import com.ieltspath.learning.application.usecase.StartPracticeAttemptUseCase;
import com.ieltspath.learning.application.usecase.SubmitLessonExerciseUseCase;
import com.ieltspath.learning.application.usecase.SubmitPracticeAttemptUseCase;
import com.ieltspath.learning.application.usecase.SubmitPracticeEssayUseCase;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PracticeStatus;
import com.ieltspath.learning.domain.vo.PracticeSubmission;
import com.ieltspath.learning.domain.vo.WritingGrade;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Essays inside practice sets: graded and charged one by one, then counted when the set is submitted. */
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
class PracticeEssayIntegrationTest {
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
    private static final UUID LESSON = UUID.randomUUID();
    private static final UUID BLOCK = UUID.randomUUID();
    private static final UUID BLOCK_QUESTION = UUID.randomUUID();
    private static final UUID KP_READING = UUID.randomUUID();
    private static final UUID KP_WRITING = UUID.randomUUID();
    private static final UUID TR_KP = UUID.randomUUID();
    private static final UUID CC_KP = UUID.randomUUID();
    private static final UUID LR_KP = UUID.randomUUID();
    private static final UUID GRA_KP = UUID.randomUUID();
    private static final UUID SUB_LINK_KP = UUID.randomUUID();
    private static final UUID SUB_TOPIC_KP = UUID.randomUUID();
    private static final UUID READING_SET = UUID.randomUUID();
    private static final UUID READING_VERSION = UUID.randomUUID();
    private static final UUID WRITING_SET = UUID.randomUUID();
    private static final UUID WRITING_VERSION = UUID.randomUUID();
    private static final UUID ESSAY = UUID.randomUUID();
    private static final List<UUID> READING_ITEMS = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    private static final String TEXT = ("Children learn better outdoors because real examples make lessons memorable "
            + "and fresh air helps them focus. ").repeat(4);

    @Autowired JdbcTemplate jdbc;
    @Autowired RefreshLearningTopicsUseCase topics;
    @Autowired SubmitLessonExerciseUseCase submitExercise;
    @Autowired GetLessonPracticeSetsUseCase practiceSets;
    @Autowired StartPracticeAttemptUseCase startPractice;
    @Autowired GetPracticeAttemptUseCase getPractice;
    @Autowired SubmitPracticeEssayUseCase submitEssay;
    @Autowired SubmitPracticeAttemptUseCase submitPractice;
    @MockitoBean LearningContentClient content;
    @MockitoBean AccessClient access;
    @MockitoBean LlmClient llm;
    @MockitoBean com.ieltspath.learning.application.port.EssayPromptActivationProvider promptActivations;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE human_review_requests, lesson_writing_submissions, llm_daily_usage, review_theory_checks, diagnostic_probes, diagnostic_sessions,review_sets, "
                + "review_items, practice_attempts, lesson_practice_passes, lesson_exercise_submissions, kp_evidence, "
                + "lesson_progress, topic_progress, knowledge_point_catalog, topic_test_assignments, "
                + "assessment_result_versions CASCADE");
        reset(content, access, llm);
        when(content.getActiveRubric(anyString())).thenAnswer(invocation -> testRubric(invocation.getArgument(0)));
        when(content.activeErrorTypes()).thenReturn(List.of(new ErrorType("GRAMMAR", "Grammar", "GRAMMAR", 1,
                "Fix grammar")));
        Set<LearningSkill> skills = Set.of(LearningSkill.READING, LearningSkill.WRITING);
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(TOPIC, "TREES", "Trees", 955, null, List.of(
                new KnowledgePoint(KP_READING, "TR_R", "Scanning", "PROCEDURE", "READING", "d", true),
                new KnowledgePoint(KP_WRITING, "TR_W", "Opinion", "PROCEDURE", "WRITING", "d", false)),
                null, true, null, skills)));
        when(content.getTopicLessons(TOPIC)).thenReturn(List.of(new LessonSummary(LESSON, TOPIC, "T1", "Trees", null,
                1, List.of(KP_READING, KP_WRITING), List.of(BLOCK), skills)));
        when(content.getLesson(LESSON)).thenReturn(new Lesson(LESSON, TOPIC, "T1", "Trees", null, 1,
                List.of(KP_READING, KP_WRITING), List.of(new Block(BLOCK, "EXERCISE", 1, null, null, null,
                List.of(new Question(BLOCK_QUESTION, 1, "Pick", List.of(new Option("A", "a", 1), new Option("B", "b", 2)),
                        Map.of("type", "CHOICE", "correct", "A"), "Because", List.of(KP_READING))), "EXERCISE")),
                null, skills));
        when(content.lessonPracticeSets(LESSON)).thenReturn(List.of(
                new LessonPracticeSet(READING_SET, READING_VERSION, "TR-R", "Reading", 3, List.of(KP_READING), null,
                        Set.of(LearningSkill.READING)),
                new LessonPracticeSet(WRITING_SET, WRITING_VERSION, "TR-W", "Writing", 1, List.of(KP_WRITING), null,
                        Set.of(LearningSkill.WRITING))));
        when(content.getPackageVersion(READING_VERSION)).thenReturn(new PackageVersion(READING_VERSION, READING_SET,
                "PRACTICE_SET", null, Map.of(), List.of(new Section(UUID.randomUUID(), "Nursery", "READING", null, 1,
                "Passage", READING_ITEMS.stream().map(id -> new Item(id, READING_ITEMS.indexOf(id) + 1, "Pick",
                        List.of(new Option("A", "a", 1), new Option("B", "b", 2)), Map.of("type", "CHOICE", "correct", "A"),
                        "Because", BigDecimal.ONE, List.of(new KnowledgePointMapping(KP_READING, BigDecimal.ONE))))
                        .toList()))));
        when(content.getPackageVersion(WRITING_VERSION)).thenReturn(new PackageVersion(WRITING_VERSION, WRITING_SET,
                "PRACTICE_SET", null, Map.of(), List.of(new Section(UUID.randomUUID(), "Opinion essay", "WRITING", null,
                1, null, List.of(new Item(ESSAY, 1, "Should children learn outdoors?", null,
                Map.of("type", "ESSAY", "task", "TASK_2", "minWords", 250, "passBand", 6.0), "Model essay",
                BigDecimal.ONE, List.of(new KnowledgePointMapping(KP_WRITING, BigDecimal.ONE))))))));
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3))).thenReturn(Map.of(KP_READING, 1));
        when(llm.configured()).thenReturn(true);
        when(access.balance()).thenReturn(30L);
        when(access.debit(any(), anyInt(), any(), anyString(), any())).thenAnswer(invocation -> UUID.randomUUID());
        topics.execute(USER);
        submitExercise.execute(USER, LESSON, BLOCK, new SubmitExerciseCommand(UUID.randomUUID(),
                List.of(new SubmitExerciseCommand.Answer(BLOCK_QUESTION, "A"))));
    }

    @Test
    void anEssayIsGradedAndChargedOnceThenCountsAsWritingWithoutAReviewOrAPracticeRequirement() {
        jdbc.update("DELETE FROM question_attempt_events WHERE user_id = ? AND question_version_id = ?", USER, ESSAY);
        var attempt = startPractice.execute(USER, LESSON, WRITING_SET).attemptId();
        assertEquals("ESSAY_NOT_GRADED", assertThrows(LearningRequestException.class,
                () -> submitPractice.execute(USER, attempt, new SubmitExerciseCommand(UUID.randomUUID(), List.of())))
                .getCode());

        when(llm.completeJson(any(), any())).thenReturn(grade("5.0"));
        UUID request = UUID.randomUUID();
        var essay = submitEssay.execute(USER, attempt, ESSAY, request, TEXT);
        assertEquals("GRADED", essay.status());
        assertFalse(essay.passed());
        assertEquals("Model essay", essay.sampleAnswer());
        assertEquals(essay.submissionId(), submitEssay.execute(USER, attempt, ESSAY, request, TEXT).submissionId());
        verify(access, times(1)).debit(eq(USER), eq(3), eq(essay.submissionId()),
                eq("practice-writing:" + USER + ":" + request), any());
        var view = getPractice.execute(USER, attempt).view();
        assertEquals("GRADED", view.sections().getFirst().essays().getFirst().latestSubmission().status());

        var result = submitPractice.execute(USER, attempt, new SubmitExerciseCommand(UUID.randomUUID(), List.of()));
        assertFalse(result.passed());
        assertEquals(List.of(new PracticeSubmission.SkillScore(LearningSkill.WRITING, 0, 1, 0.0, false)),
                result.skillScores());
        assertTrue(result.reviewsCreated().isEmpty());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE source = 'practice_set' "
                + "AND kp_id = ? AND correct = false", Integer.class, KP_WRITING));
        // The essay is one question event of the submission, and every question event is a day of activity.
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM question_attempt_events WHERE user_id = ? "
                + "AND question_version_id = ? AND source = 'practice_set'", Integer.class, USER, ESSAY));
        assertEquals(PracticeStatus.REQUIRED, practiceSets.execute(USER, LESSON).practiceStatus());

        var reading = startPractice.execute(USER, LESSON, READING_SET).attemptId();
        submitPractice.execute(USER, reading, new SubmitExerciseCommand(UUID.randomUUID(), READING_ITEMS.stream()
                .map(id -> new SubmitExerciseCommand.Answer(id, "A")).toList()));
        assertEquals(PracticeStatus.PASSED, practiceSets.execute(USER, LESSON).practiceStatus());
    }

    @Test
    void essaysNeedPointsAndQuotaAndOnlyEssayItemsAccept() {
        var attempt = startPractice.execute(USER, LESSON, WRITING_SET).attemptId();
        var reading = startPractice.execute(USER, LESSON, READING_SET).attemptId();
        assertEquals("NOT_ESSAY_ITEM", assertThrows(LearningRequestException.class,
                () -> submitEssay.execute(USER, reading, READING_ITEMS.getFirst(), UUID.randomUUID(), TEXT)).getCode());

        when(access.balance()).thenReturn(0L);
        assertEquals("INSUFFICIENT_POINTS", assertThrows(LearningRequestException.class,
                () -> submitEssay.execute(USER, attempt, ESSAY, UUID.randomUUID(), TEXT)).getCode());

        when(access.balance()).thenReturn(30L);
        jdbc.update("INSERT INTO llm_daily_usage (user_id, usage_date, kind, count) VALUES (?, ?, 'writing_grading', 10)",
                USER, LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        assertEquals("DAILY_LIMIT_REACHED", assertThrows(LearningRequestException.class,
                () -> submitEssay.execute(USER, attempt, ESSAY, UUID.randomUUID(), TEXT)).getCode());
        verify(llm, never()).completeJson(any(), any());
    }

    @Test
    void aPassedEssayReleasesItsPointsOnlyWhenTheDebitSucceeds() {
        var attempt = startPractice.execute(USER, LESSON, WRITING_SET).attemptId();
        when(llm.completeJson(any(), any())).thenReturn(grade("6.5"));
        when(access.debit(any(), anyInt(), any(), anyString(), any())).thenThrow(new InsufficientPointsException());
        UUID request = UUID.randomUUID();
        assertEquals("INSUFFICIENT_POINTS", assertThrows(LearningRequestException.class,
                () -> submitEssay.execute(USER, attempt, ESSAY, request, TEXT)).getCode());

        reset(access);
        when(access.debit(any(), anyInt(), any(), anyString(), any())).thenAnswer(invocation -> UUID.randomUUID());
        var essay = submitEssay.execute(USER, attempt, ESSAY, request, TEXT);
        assertTrue(essay.passed());
        assertEquals("IELTS_WRITING@1+tplv1", jdbc.queryForObject(
                "SELECT prompt_version FROM lesson_writing_submissions", String.class));
        verify(llm, times(1)).completeJson(any(), any());
        var result = submitPractice.execute(USER, attempt, new SubmitExerciseCommand(UUID.randomUUID(), List.of()));
        assertTrue(result.passed());
    }

    @Test
    void aCountedPracticeEssayRecordsEachCriterionOnItsOwnKnowledgePoint() {
        jdbc.update("DELETE FROM question_attempt_events WHERE user_id = ?", USER);
        jdbc.update("DELETE FROM kp_evidence WHERE user_id = ?", USER);
        when(content.getActiveRubric(anyString())).thenReturn(rubricWithCriterionPoints());
        var attempt = startPractice.execute(USER, LESSON, WRITING_SET).attemptId();
        when(llm.completeJson(any(), any())).thenReturn(gradeWithBands("7.0", "5.5", "6.5", "6.0"));
        var essay = submitEssay.execute(USER, attempt, ESSAY, UUID.randomUUID(), TEXT);
        assertEquals("GRADED", essay.status());

        var result = submitPractice.execute(USER, attempt, new SubmitExerciseCommand(UUID.randomUUID(), List.of()));

        assertTrue(result.countedAsEvidence());
        Map<UUID, Boolean> recorded = new HashMap<>();
        jdbc.queryForList("SELECT kp_id, correct FROM kp_evidence WHERE source = 'practice_set' "
                + "AND question_version_id = ?", ESSAY)
                .forEach(row -> recorded.put((UUID) row.get("kp_id"), (Boolean) row.get("correct")));
        assertEquals(Map.of(KP_WRITING, Boolean.TRUE.equals(essay.passed()), TR_KP, true, CC_KP, false, LR_KP, true,
                GRA_KP, true), recorded);
        assertEquals("GRAMMAR", jdbc.queryForObject("SELECT error_code FROM question_attempt_events "
                + "WHERE user_id = ? AND question_version_id = ? AND source = 'practice_set'", String.class, USER, ESSAY));
    }

    @Test
    void aFailedSubKnowledgePointAddsPracticeEvidenceAndAnUnassessedOneDoesNot() {
        when(content.getActiveRubric(anyString())).thenReturn(rubricWithSubKnowledgePoints());
        when(content.getTopicSequence()).thenReturn(List.of(new Topic(TOPIC, "TREES", "Trees", 955, null, List.of(
                new KnowledgePoint(KP_READING, "TR_R", "Scanning", "PROCEDURE", "READING", "d", true),
                new KnowledgePoint(KP_WRITING, "TR_W", "Opinion", "PROCEDURE", "WRITING", "d", false),
                new KnowledgePoint(SUB_LINK_KP, "DEMO_WRITING_LINKING", "Linking", "PROCEDURE", "WRITING",
                        "Connect ideas", true),
                new KnowledgePoint(SUB_TOPIC_KP, "DEMO_WRITING_TOPIC_SENTENCE", "Topic sentence", "PROCEDURE",
                        "WRITING", "State the main idea", true)), null, true, null, Set.of(LearningSkill.READING,
                LearningSkill.WRITING))));
        var attempt = startPractice.execute(USER, LESSON, WRITING_SET).attemptId();
        when(llm.completeJson(any(), any())).thenReturn(gradeWithBands("7.0", "5.5", "6.5", "6.0")
                .replace("\"focus\":[]", "\"focus\":[],\"judgments\":[{\"criterion\":\"CC\","
                + "\"code\":\"DEMO_WRITING_LINKING\",\"judgment\":\"FAIL\","
                + "\"excerpt\":\"because real examples make lessons memorable\",\"reason\":\"Use a linker clearly.\"}]"));
        var essay = submitEssay.execute(USER, attempt, ESSAY, UUID.randomUUID(), TEXT);
        UUID requestId = UUID.randomUUID();

        submitPractice.execute(USER, attempt, new SubmitExerciseCommand(requestId, List.of()));
        submitPractice.execute(USER, attempt, new SubmitExerciseCommand(requestId, List.of()));

        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE source = 'practice_set' "
                + "AND question_version_id = ? AND kp_id = ? AND correct = false", Integer.class, ESSAY, SUB_LINK_KP));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE source = 'practice_set' "
                + "AND question_version_id = ? AND kp_id = ?", Integer.class, ESSAY, SUB_TOPIC_KP));
        assertTrue(essay.grade().studyPoints().stream().anyMatch(point ->
                WritingGrade.StudyPoint.KNOWLEDGE_POINT.equals(point.kind())
                        && SUB_LINK_KP.equals(point.practiceKnowledgePointId())));
        verify(llm, times(1)).completeJson(any(), any());
    }

    private static String gradeWithBands(String tr, String cc, String lr, String gra) {
        return "{\"criteria\":[" + bandJson("TR", tr) + "," + bandJson("CC", cc) + "," + bandJson("LR", lr) + ","
                + bandJson("GRA", gra) + "],\"corrections\":[{\"excerpt\":\"Children learn better outdoors\","
                + "\"suggestion\":\"fix\",\"errorCode\":\"GRAMMAR\"}],\"summary\":\"ok\",\"focus\":[]}";
    }

    private static String bandJson(String code, String band) {
        return "{\"code\":\"%s\",\"band\":%s,\"comment\":\"i\"}".formatted(code, band);
    }

    private static Rubric rubricWithCriterionPoints() {
        var descriptor = new RubricBandDescriptor(new BigDecimal("6.0"), "Band six.");
        Map<String, UUID> points = Map.of("TR", TR_KP, "CC", CC_KP, "LR", LR_KP, "GRA", GRA_KP);
        var criteria = List.of("TR", "CC", "LR", "GRA").stream()
                .map(code -> new RubricCriterion(code, code, "Description", 1, List.of(descriptor), points.get(code)))
                .toList();
        return new Rubric("IELTS_WRITING", "TASK_2", 1, "source", criteria, List.of());
    }

    private static Rubric rubricWithSubKnowledgePoints() {
        var descriptor = new RubricBandDescriptor(new BigDecimal("6.0"), "Band six.");
        Map<String, UUID> points = Map.of("TR", TR_KP, "CC", CC_KP, "LR", LR_KP, "GRA", GRA_KP);
        var criteria = List.of("TR", "CC", "LR", "GRA").stream()
                .map(code -> new RubricCriterion(code, code, "Description", 1, List.of(descriptor), points.get(code),
                        code.equals("CC") ? List.of(
                                new LearningContentClient.SubKnowledgePoint(SUB_TOPIC_KP,
                                        "DEMO_WRITING_TOPIC_SENTENCE", "Topic sentence", "State the main idea"),
                                new LearningContentClient.SubKnowledgePoint(SUB_LINK_KP, "DEMO_WRITING_LINKING",
                                        "Linking", "Connect ideas")) : List.of())).toList();
        return new Rubric("IELTS_WRITING", "TASK_2", 1, "source", criteria, List.of());
    }

    private static String grade(String band) {
        String criteria = String.join(",", List.of("TR", "CC", "LR", "GRA").stream()
                .map(code -> "{\"code\":\"%s\",\"band\":%s,\"comment\":\"i\"}"
                        .formatted(code, band)).toList());
        return "{\"criteria\":[" + criteria + "],\"corrections\":[],\"summary\":\"ok\",\"focus\":[]}";
    }

    private static Rubric testRubric(String task) {
        var descriptor = new RubricBandDescriptor(new BigDecimal("6.0"), "Band six.");
        var criteria = (task.equals("TASK_1") ? List.of("TA", "CC", "LR", "GRA") : List.of("TR", "CC", "LR", "GRA"))
                .stream().map(code -> new RubricCriterion(code, code, "Description", 1, List.of(descriptor))).toList();
        return new Rubric("IELTS_WRITING", task, 1, "source", criteria, List.of());
    }
}
