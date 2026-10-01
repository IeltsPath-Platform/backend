package com.group01.learning.infrastructure.persistence;

import com.group01.learning.application.exception.AccessUnavailableException;
import com.group01.learning.application.exception.InsufficientPointsException;
import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.exception.LlmUnavailableException;
import com.group01.learning.application.port.AccessClient;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.port.LlmClient;
import com.group01.learning.application.result.LessonResult;
import com.group01.learning.application.result.WritingSubmissionResult;
import com.group01.learning.application.usecase.LearnLessonUseCase;
import com.group01.learning.application.usecase.LessonEssayUseCase;
import com.group01.learning.application.usecase.RefreshLearningTopicsUseCase;
import com.group01.learning.domain.exception.LearningGateException;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** The essay flow against real PostgreSQL; Content, Access and the LLM are stubs. */
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
class LessonWritingIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db")
            .withPassword(UUID.randomUUID().toString());

    private static final byte[] INTERNAL_KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    private static final UUID USER = UUID.randomUUID();
    private static final UUID OTHER_USER = UUID.randomUUID();
    private static final UUID TOPIC = UUID.randomUUID();
    private static final UUID LESSON = UUID.randomUUID();
    private static final UUID EXERCISE_BLOCK = UUID.randomUUID();
    private static final UUID ESSAY_BLOCK = UUID.randomUUID();
    private static final UUID KP = UUID.randomUUID();
    private static final UUID ESSAY_KP = UUID.randomUUID();
    private static final String ESSAY = ("Cities should plant more trees because trees cool streets, clean the air "
            + "and make people calmer. ").repeat(4);

    @Autowired JdbcTemplate jdbc;
    @Autowired LessonEssayUseCase essays;
    @Autowired LearnLessonUseCase lessons;
    @Autowired RefreshLearningTopicsUseCase topics;
    @MockitoBean LearningContentClient content;
    @MockitoBean AccessClient access;
    @MockitoBean LlmClient llm;

    @BeforeEach
    void seed() {
        jdbc.execute("""
                TRUNCATE review_sets, review_items, lesson_exercise_submissions, kp_evidence, lesson_progress,
                topic_progress, knowledge_point_catalog, topic_test_assignments, assessment_result_versions,
                lesson_writing_submissions, llm_daily_usage RESTART IDENTITY
                """);
        reset(content, access, llm);
        when(content.getTopicSequence()).thenReturn(List.of(new LearningContentClient.Topic(TOPIC, "WRITING",
                "Writing", 900, List.of(kp(KP), kp(ESSAY_KP)))));
        when(content.getTopicLessons(TOPIC)).thenReturn(List.of(new LearningContentClient.LessonSummary(LESSON, TOPIC,
                "L4", "Essay lesson", null, 1, List.of(KP, ESSAY_KP), List.of(EXERCISE_BLOCK))));
        var exercise = new LearningContentClient.Block(EXERCISE_BLOCK, "EXERCISE", 1, null, null, null,
                List.of(new LearningContentClient.Question(UUID.randomUUID(), 1, "Pick A",
                        List.of(new LearningContentClient.Option("A", "a", 1), new LearningContentClient.Option("B", "b", 2)),
                        Map.of("type", "CHOICE", "correct", "A"), null, List.of(KP))), "EXERCISE");
        var essay = new LearningContentClient.Block(ESSAY_BLOCK, "EXERCISE", 2, null, null, null,
                List.of(new LearningContentClient.Question(UUID.randomUUID(), 1, "Should cities plant more trees?",
                        null, Map.of("type", "ESSAY", "task", "TASK_2", "minWords", 250, "passBand", 6.0),
                        "Model essay", List.of(ESSAY_KP))), "ESSAY");
        when(content.getLesson(LESSON)).thenReturn(new LearningContentClient.Lesson(LESSON, TOPIC, "L4",
                "Essay lesson", null, 1, List.of(KP, ESSAY_KP), List.of(exercise, essay)));
        when(llm.configured()).thenReturn(true);
        when(access.balance()).thenReturn(30L);
        when(access.debit(any(), anyInt(), any(), anyString(), any())).thenAnswer(invocation -> UUID.randomUUID());
        topics.execute(USER);
    }

    @Test
    void gradedEssayChargesOnceRecordsEvidenceAndAResendReplaysIt() {
        llmReplies(grade("6.5"));
        UUID request = UUID.randomUUID();

        WritingSubmissionResult result = essays.submit(USER, LESSON, ESSAY_BLOCK, request, ESSAY);

        assertEquals("GRADED", result.status());
        assertEquals(new BigDecimal("6.5"), result.overallBand());
        assertTrue(result.passed());
        assertEquals(3, result.pointsCharged());
        assertEquals("Model essay", result.sampleAnswer());
        assertEquals(List.of("TR", "CC", "LR", "GRA"), result.grade().criteria().stream().map(c -> c.code()).toList());
        verify(access).debit(eq(USER), eq(3), eq(result.submissionId()), eq("lesson-writing:" + USER + ":" + request),
                any());
        assertEquals(List.of(Map.of("source", "lesson_writing", "kp_id", ESSAY_KP, "correct", true)), jdbc.queryForList(
                "SELECT source, kp_id, correct FROM kp_evidence"));

        clearInvocations(llm, access, content);
        assertEquals(result, essays.submit(USER, LESSON, ESSAY_BLOCK, request, ESSAY));
        verifyNoInteractions(llm, access, content);
        assertEquals(result, essays.get(USER, result.submissionId()));
        assertEquals(404, assertThrows(LearningRequestException.class,
                () -> essays.get(OTHER_USER, result.submissionId())).getStatus());
    }

    @Test
    void essayValidationAndBalanceRunBeforeAnyRowOrLlmCall() {
        assertCode(422, "ESSAY_EMPTY", () -> essays.submit(USER, LESSON, ESSAY_BLOCK, UUID.randomUUID(), "  "));
        assertCode(422, "ESSAY_TOO_SHORT", () -> essays.submit(USER, LESSON, ESSAY_BLOCK, UUID.randomUUID(),
                "Too short."));
        assertCode(422, "ESSAY_TOO_LONG", () -> essays.submit(USER, LESSON, ESSAY_BLOCK, UUID.randomUUID(),
                "word ".repeat(1001)));
        when(access.balance()).thenReturn(2L);
        assertCode(402, "INSUFFICIENT_POINTS", () -> essays.submit(USER, LESSON, ESSAY_BLOCK, UUID.randomUUID(), ESSAY));

        assertEquals(0, count("lesson_writing_submissions"));
        verify(llm, never()).completeJson(any(), any(), anyBoolean());
        verify(access, never()).debit(any(), anyInt(), any(), any(), any());
    }

    @Test
    void llmFailureChargesNothingAndAResendGradesTheSameSubmission() {
        when(llm.completeJson(any(), any(), anyBoolean())).thenThrow(new LlmUnavailableException(500));
        UUID request = UUID.randomUUID();

        assertCode(503, "GRADING_UNAVAILABLE", () -> essays.submit(USER, LESSON, ESSAY_BLOCK, request, ESSAY));
        assertEquals("FAILED", status());
        assertEquals("LLM_UNAVAILABLE", jdbc.queryForObject("SELECT failure_code FROM lesson_writing_submissions",
                String.class));
        verify(access, never()).debit(any(), anyInt(), any(), any(), any());

        reset(llm);
        when(llm.configured()).thenReturn(true);
        llmReplies(grade("5.5"));
        WritingSubmissionResult retried = essays.submit(USER, LESSON, ESSAY_BLOCK, request, ESSAY);
        assertEquals("GRADED", retried.status());
        assertFalse(retried.passed());
        assertNull(retried.sampleAnswer());
        assertEquals(1, count("lesson_writing_submissions"));
    }

    @Test
    void aRefusedDebitWithholdsTheGradeUntilAResendPays() {
        llmReplies(grade("7"));
        when(access.debit(any(), anyInt(), any(), anyString(), any())).thenThrow(new InsufficientPointsException());
        UUID request = UUID.randomUUID();

        LearningRequestException refused = assertCode(402, "INSUFFICIENT_POINTS",
                () -> essays.submit(USER, LESSON, ESSAY_BLOCK, request, ESSAY));
        UUID submission = refused.getSubmissionId();
        assertNotNull(submission);
        WritingSubmissionResult pending = essays.get(USER, submission);
        assertEquals("PAYMENT_PENDING", pending.status());
        assertEquals("INSUFFICIENT_POINTS", pending.code());
        assertNull(pending.overallBand());
        assertNull(pending.grade());
        LessonResult.Block block = essayBlock(lessons.get(USER, LESSON));
        assertEquals("PAYMENT_PENDING", block.latestSubmission().status());
        assertNull(block.latestSubmission().overallBand(), "a withheld grade does not leak through the lesson");
        assertEquals(0, count("kp_evidence WHERE source = 'lesson_writing'"));

        reset(access);
        when(access.debit(any(), anyInt(), any(), anyString(), any())).thenThrow(new AccessUnavailableException(401));
        assertCode(503, "PAYMENT_UNAVAILABLE", () -> essays.submit(USER, LESSON, ESSAY_BLOCK, request, ESSAY));

        reset(access, llm);
        UUID ledger = UUID.randomUUID();
        when(access.debit(any(), anyInt(), any(), anyString(), any())).thenReturn(ledger);
        WritingSubmissionResult paid = essays.submit(USER, LESSON, ESSAY_BLOCK, request, ESSAY);
        assertEquals("GRADED", paid.status());
        assertEquals(new BigDecimal("7.0"), paid.overallBand());
        verifyNoInteractions(llm);
        verify(access, never()).balance();
        assertEquals(ledger, jdbc.queryForObject("SELECT debit_ledger_entry_id FROM lesson_writing_submissions",
                UUID.class));
        assertEquals(1, count("kp_evidence WHERE source = 'lesson_writing'"));
    }

    @Test
    void evidenceIsRecordedForEveryEssayUntilTheBlockIsFirstPassed() {
        llmReplies(grade("5"), grade("6.5"), grade("8"));
        for (int i = 0; i < 3; i++) essays.submit(USER, LESSON, ESSAY_BLOCK, UUID.randomUUID(), ESSAY);

        assertEquals(List.of(false, true), jdbc.queryForList(
                "SELECT correct FROM kp_evidence WHERE source = 'lesson_writing' ORDER BY ordinal", Boolean.class));
        assertEquals(0, count("review_items"), "essays never insert reviews");
        LessonResult.Block block = essayBlock(lessons.get(USER, LESSON));
        assertEquals("Model essay", block.sampleAnswer());
        assertEquals(new BigDecimal("8.0"), block.latestSubmission().overallBand());
    }

    @Test
    void dailyLimitStopsGradingBeforeTheLlm() {
        jdbc.update("INSERT INTO llm_daily_usage (user_id, usage_date, kind, count) VALUES (?, ?, 'writing_grading', 10)",
                USER, LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        assertCode(429, "DAILY_LIMIT_REACHED", () -> essays.submit(USER, LESSON, ESSAY_BLOCK, UUID.randomUUID(), ESSAY));
        verify(llm, never()).completeJson(any(), any(), anyBoolean());
        assertEquals("FAILED", status());
    }

    @Test
    void lessonGatesWrongBlocksAndForeignRequestsAreRejected() {
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status) VALUES (?, ?, ?, ?, 'PENDING')",
                UUID.randomUUID(), USER, KP, LESSON);
        assertEquals("REVIEW_REQUIRED", assertThrows(LearningGateException.class,
                () -> essays.submit(USER, LESSON, ESSAY_BLOCK, UUID.randomUUID(), ESSAY)).getCode());
        assertEquals(0, count("lesson_writing_submissions"));
        jdbc.update("DELETE FROM review_items");

        assertCode(409, "NOT_ESSAY_BLOCK", () -> essays.submit(USER, LESSON, EXERCISE_BLOCK, UUID.randomUUID(), ESSAY));

        llmReplies(grade("6"));
        UUID request = UUID.randomUUID();
        essays.submit(USER, LESSON, ESSAY_BLOCK, request, ESSAY);
        assertCode(409, "REQUEST_CONFLICT", () -> essays.submit(OTHER_USER, LESSON, ESSAY_BLOCK, request, ESSAY));
    }

    @Test
    void aStaleGradingNoLongerBlocksTheBlockButAFreshOneDoes() {
        UUID abandoned = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO lesson_writing_submissions (id, user_id, lesson_id, block_id, question_version_id,
                knowledge_point_ids, request_id, essay_text, word_count, prompt_snapshot, status, point_cost,
                grading_started_at, submitted_at)
                VALUES (?, ?, ?, ?, ?, ARRAY[]::uuid[], ?, 'x', 1, '{}'::jsonb, 'GRADING', 3,
                now() - interval '10 minutes', now() - interval '10 minutes')
                """, abandoned, USER, LESSON, ESSAY_BLOCK, UUID.randomUUID(), UUID.randomUUID());

        llmReplies(grade("6"));
        assertEquals("GRADED", essays.submit(USER, LESSON, ESSAY_BLOCK, UUID.randomUUID(), ESSAY).status());
        assertEquals("GRADING_ABANDONED", jdbc.queryForObject(
                "SELECT failure_code FROM lesson_writing_submissions WHERE id = ?", String.class, abandoned));

        jdbc.update("UPDATE lesson_writing_submissions SET status = 'GRADING', grading_started_at = now() WHERE id = ?",
                abandoned);
        assertCode(409, "GRADING_IN_PROGRESS", () -> essays.submit(USER, LESSON, ESSAY_BLOCK, UUID.randomUUID(), ESSAY));
    }

    @Test
    void whileOneEssayIsGradingAnotherOfTheSameBlockIsRefused() throws Exception {
        var grading = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        when(llm.completeJson(any(), any(), anyBoolean())).thenAnswer(invocation -> {
            grading.countDown();
            assertTrue(release.await(20, java.util.concurrent.TimeUnit.SECONDS));
            return grade("6");
        });
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var first = executor.submit(() -> essays.submit(USER, LESSON, ESSAY_BLOCK, UUID.randomUUID(), ESSAY));
            assertTrue(grading.await(20, java.util.concurrent.TimeUnit.SECONDS));
            assertCode(409, "GRADING_IN_PROGRESS",
                    () -> essays.submit(USER, LESSON, ESSAY_BLOCK, UUID.randomUUID(), ESSAY));
            release.countDown();
            assertEquals("GRADED", first.get(30, java.util.concurrent.TimeUnit.SECONDS).status());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
        assertEquals(1, count("lesson_writing_submissions"));
        verify(access, times(1)).debit(any(), anyInt(), any(), anyString(), any());
    }

    @Test
    void theEssayBlockNeverBlocksLessonCompletion() {
        var lesson = lessons.get(USER, LESSON);
        assertNull(essayBlock(lesson).latestSubmission());
        assertNull(essayBlock(lesson).sampleAnswer());
        var exercise = lesson.blocks().stream().filter(block -> "EXERCISE".equals(block.blockKind())).findFirst()
                .orElseThrow();
        var submitted = lessons.submit(USER, LESSON, EXERCISE_BLOCK, new com.group01.learning.application.command
                .SubmitExerciseCommand(UUID.randomUUID(), List.of(new com.group01.learning.application.command
                .SubmitExerciseCommand.Answer(exercise.questions().getFirst().questionVersionId(), "A"))));
        assertTrue(submitted.lessonCompleted());
        assertCode(409, "ESSAY_BLOCK", () -> lessons.submit(USER, LESSON, ESSAY_BLOCK,
                new com.group01.learning.application.command.SubmitExerciseCommand(UUID.randomUUID(), List.of())));
    }

    private void llmReplies(String first, String... rest) {
        when(llm.completeJson(any(), any(), anyBoolean())).thenReturn(first, rest);
    }

    private static String grade(String band) {
        String criteria = String.join(",", List.of("TR", "CC", "LR", "GRA").stream()
                .map(code -> "{\"code\":\"%s\",\"band\":%s,\"strengths\":[\"s\"],\"improvements\":[\"i\"]}"
                        .formatted(code, band)).toList());
        return "{\"criteria\":[" + criteria + "],\"corrections\":[],\"summary\":\"ok\"}";
    }

    private static LearningContentClient.KnowledgePoint kp(UUID id) {
        return new LearningContentClient.KnowledgePoint(id, "KP", "Knowledge point", "PROCEDURE", "WRITING", null,
                false);
    }

    private static LessonResult.Block essayBlock(LessonResult lesson) {
        return lesson.blocks().stream().filter(block -> "ESSAY".equals(block.blockKind())).findFirst().orElseThrow();
    }

    private static LearningRequestException assertCode(int status, String code,
                                                       org.junit.jupiter.api.function.Executable call) {
        LearningRequestException exception = assertThrows(LearningRequestException.class, call);
        assertEquals(status, exception.getStatus());
        assertEquals(code, exception.getCode());
        return exception;
    }

    private String status() {
        return jdbc.queryForObject("SELECT status FROM lesson_writing_submissions", String.class);
    }

    private int count(String tableAndFilter) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tableAndFilter, Integer.class);
    }
}
