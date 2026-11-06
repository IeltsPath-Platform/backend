package com.ieltspath.learning.infrastructure.persistence;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.ieltspath.learning.application.usecase.SubmitPracticeAttemptUseCase;
import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.command.HumanReviewResult;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.HumanReviewClient;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.service.PracticeAccess;
import com.ieltspath.learning.application.usecase.ApplyHumanReviewResultUseCase;
import com.ieltspath.learning.application.usecase.SubmitPracticeEssayToExaminerUseCase;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * An essay of a lesson's practice set sent straight to an examiner: stored with its lesson, the review asked for once, the
 * AI kept off the same question, and the examiner's grade written into the learner's mastery like a lesson essay's.
 */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class PracticeEssayToExaminerIntegrationTest {
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

    /** About a hundred words: long enough for any essay limit of the service. */
    private static final String ESSAY = ("Cities should plant more trees because trees cool the streets in summer and "
            + "clean the air that people breathe every day. ").repeat(5).trim();

    @Autowired JdbcTemplate jdbc;
    @Autowired SubmitPracticeEssayToExaminerUseCase sendToExaminer;
    @Autowired SubmitPracticeAttemptUseCase submitAttempt;
    @Autowired ApplyHumanReviewResultUseCase applyResult;
    @MockitoBean HumanReviewClient client;
    @MockitoBean LearningContentClient content;
    /** The lesson gate (lesson completed, no blocking review) is covered elsewhere; here it lets the learner in. */
    @MockitoBean PracticeAccess access;

    private final UUID user = UUID.randomUUID();
    private final UUID examiner = UUID.randomUUID();
    private final UUID lesson = UUID.randomUUID();
    private final UUID attempt = UUID.randomUUID();
    private final UUID version = UUID.randomUUID();
    private final UUID question = UUID.randomUUID();
    private final UUID review = UUID.randomUUID();
    private final UUID taskResponse = UUID.randomUUID(), coherence = UUID.randomUUID();
    private final UUID linkingPoint = UUID.randomUUID(), linkerTrap = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE human_review_outcomes, human_review_requests, lesson_writing_submissions, kp_evidence, "
                + "review_items, knowledge_point_catalog, practice_attempts CASCADE");
        reset(client, content, access);
        jdbc.update("INSERT INTO practice_attempts (id, user_id, lesson_id, skill, skills, package_id, package_version_id) "
                + "VALUES (?, ?, ?, 'WRITING', ARRAY['WRITING'], ?, ?)", attempt, user, lesson, UUID.randomUUID(), version);
        Map<String, Object> spec = Map.of("type", "ESSAY", "task", "TASK_2", "minWords", 250, "passBand", 6.0);
        var item = new LearningContentClient.Item(question, 1, "Should cities plant more trees?", null, spec, "Sample",
                null, BigDecimal.ONE, List.of(), null, List.of());
        when(content.getPackageVersion(version)).thenReturn(new LearningContentClient.PackageVersion(version,
                UUID.randomUUID(), "PRACTICE_SET", null, Map.of(),
                List.of(new LearningContentClient.Section(UUID.randomUUID(), "Write", "WRITING", "", 1, null, List.of(item)))));
        when(content.getActiveRubric(any())).thenReturn(new LearningContentClient.Rubric("IELTS_WRITING_TASK_2", "TASK_2", 1,
                "source", List.of(
                new LearningContentClient.RubricCriterion("TR", "Task response", "", 1, List.of(), taskResponse),
                new LearningContentClient.RubricCriterion("CC", "Coherence", "", 2, List.of(), coherence)), List.of()));
        when(content.activeErrorTypes()).thenReturn(List.of(
                new LearningContentClient.ErrorType("LINKER_MISUSE", "Linker", "COHERENCE", 1, "Use the right linker")));
        when(client.consumeCredit(any())).thenReturn(new HumanReviewClient.CreditCharge(4, true));
        when(client.createReview(any())).thenReturn(review);
        catalogPoint(taskResponse, null, null);
        catalogPoint(coherence, null, null);
        catalogPoint(linkingPoint, null, null);
        catalogPoint(linkerTrap, linkingPoint, "LINKER_MISUSE");
    }

    private void catalogPoint(UUID point, UUID parent, String errorCode) {
        jdbc.update("""
                INSERT INTO knowledge_point_catalog (kp_id, topic_id, has_practice_set, skill, parent_kp_id, error_code)
                VALUES (?, ?, ?, 'WRITING', ?, ?)
                """, point, UUID.randomUUID(), parent == null, parent, errorCode);
    }

    private int count(String sql, Object... arguments) {
        return jdbc.queryForObject(sql, Integer.class, arguments);
    }

    @Test
    void theEssayIsKeptWithItsLessonAndTheChosenExaminerIsAskedOnce() {
        UUID requestId = UUID.randomUUID();

        var sent = sendToExaminer.execute(user, attempt, question, requestId, ESSAY, examiner);

        assertEquals("AWAITING_EXAMINER", sent.status());
        Map<String, Object> row = jdbc.queryForMap("SELECT source, lesson_id, practice_attempt_id, status, point_cost, "
                + "prompt_snapshot->>'passBand' AS pass_band FROM lesson_writing_submissions WHERE id = ?", sent.submissionId());
        assertEquals("PRACTICE_ITEM", row.get("source"));
        assertEquals(lesson, row.get("lesson_id"), "the lesson stays with the essay, so the grade counts toward mastery");
        assertEquals(attempt, row.get("practice_attempt_id"));
        assertEquals(0, row.get("point_cost"), "no points: the examiner is paid with a Premium credit");
        assertEquals("6.0", row.get("pass_band"));
        var asked = org.mockito.ArgumentCaptor.forClass(HumanReviewClient.CreateReview.class);
        verify(client).createReview(asked.capture());
        assertEquals(examiner, asked.getValue().examinerId());
        assertEquals("PRACTICE", asked.getValue().source());
        assertEquals(ESSAY, asked.getValue().essay());

        // A resend of the same request continues the same essay: nothing new is stored, spent or asked.
        assertEquals(sent.submissionId(), sendToExaminer.execute(user, attempt, question, requestId, ESSAY, examiner).submissionId());
        assertEquals(1, count("SELECT count(*) FROM lesson_writing_submissions"));
        assertEquals(1, count("SELECT count(*) FROM human_review_requests"));
        verify(client, times(1)).createReview(any());
    }

    @Test
    void aSecondEssayForTheSameQuestionIsRefusedWhileTheExaminerHasTheFirst() {
        var first = sendToExaminer.execute(user, attempt, question, UUID.randomUUID(), ESSAY, examiner);

        var refused = assertThrows(LearningRequestException.class,
                () -> sendToExaminer.execute(user, attempt, question, UUID.randomUUID(), ESSAY + " Again.", examiner));

        assertEquals(409, refused.getStatus());
        assertEquals("EXAMINER_REVIEW_PENDING", refused.getCode());
        assertEquals(first.submissionId(), refused.getSubmissionId(), "the learner is pointed at the essay that waits");
        assertEquals(1, count("SELECT count(*) FROM lesson_writing_submissions"));
    }

    @Test
    void anEssayTheAiAlreadyGradedIsNotSentAsANewOne() {
        jdbc.update("""
                INSERT INTO lesson_writing_submissions (id, user_id, lesson_id, practice_attempt_id, source,
                    question_version_id, knowledge_point_ids, request_id, essay_text, word_count, prompt_snapshot, status,
                    point_cost, grading_started_at, submitted_at, result, overall_band, passed, debit_ledger_entry_id)
                VALUES (?, ?, ?, ?, 'PRACTICE_ITEM', ?, ARRAY[]::uuid[], ?, 'Graded essay', 2,
                    CAST('{"task":"TASK_2"}' AS jsonb), 'GRADED', 3, now(), now(),
                    CAST('{"criteria":[],"corrections":[],"summary":"ok"}' AS jsonb), 6.0, true, ?)
                """, UUID.randomUUID(), user, lesson, attempt, question, UUID.randomUUID(), UUID.randomUUID());

        var refused = assertThrows(LearningRequestException.class,
                () -> sendToExaminer.execute(user, attempt, question, UUID.randomUUID(), ESSAY, examiner));

        assertEquals("ESSAY_ALREADY_GRADED", refused.getCode());
        verify(client, times(0)).createReview(any());
    }

    @Test
    void anEssayOfAnotherLearnersAttemptIsRefused() {
        var refused = assertThrows(LearningRequestException.class,
                () -> sendToExaminer.execute(UUID.randomUUID(), attempt, question, UUID.randomUUID(), ESSAY, examiner));

        assertEquals(404, refused.getStatus());
        assertEquals(0, count("SELECT count(*) FROM lesson_writing_submissions"));
    }

    /**
     * The examiner's grade of a practice essay sent straight to them is written like any lesson essay's: each criterion
     * against the prompt's pass band, the marked error as a wrong answer on its trap point, once only.
     */
    @Test
    void theExaminersGradeOfThePracticeEssayIsWrittenIntoMasteryOnce() {
        var sent = sendToExaminer.execute(user, attempt, question, UUID.randomUUID(), ESSAY, examiner);
        HumanReviewResult result = new HumanReviewResult(UUID.randomUUID(), user, review, sent.submissionId(),
                List.of(new HumanReviewResult.Criterion("taskResponse", new BigDecimal("6.5")),
                        new HumanReviewResult.Criterion("coherence", new BigDecimal("5.0"))),
                List.of(new HumanReviewResult.ErrorCount("LINKER_MISUSE", 2)));

        applyResult.execute(result);

        assertEquals(true, jdbc.queryForObject("SELECT correct FROM kp_evidence WHERE source = 'human_review' AND kp_id = ?",
                Boolean.class, taskResponse), "6.5 reaches the pass band 6.0");
        assertEquals(false, jdbc.queryForObject("SELECT correct FROM kp_evidence WHERE source = 'human_review' AND kp_id = ?",
                Boolean.class, coherence), "5.0 is below the pass band 6.0");
        assertEquals(1, count("SELECT count(*) FROM kp_evidence WHERE source = 'error_trap' AND kp_id = ? AND NOT correct",
                linkerTrap), "the marked error is one wrong answer on its trap point");
        assertEquals(1, count("SELECT count(*) FROM human_review_outcomes WHERE submission_id = ?", sent.submissionId()));

        applyResult.execute(result);

        assertEquals(2, count("SELECT count(*) FROM kp_evidence WHERE source = 'human_review'"), "a repeated event adds nothing");
        assertEquals(1, count("SELECT count(*) FROM kp_evidence WHERE source = 'error_trap'"));
    }

    /** The examiner's result for the sent essay: task response 6.5 and coherence 5.0 (a mean of 6.0 against the pass band 6.0). */
    private HumanReviewResult examinerResult(UUID submission, String taskBand, String coherenceBand) {
        return new HumanReviewResult(UUID.randomUUID(), user, review, submission,
                List.of(new HumanReviewResult.Criterion("taskResponse", new BigDecimal(taskBand)),
                        new HumanReviewResult.Criterion("coherence", new BigDecimal(coherenceBand))),
                List.of(new HumanReviewResult.ErrorCount("LINKER_MISUSE", 2)));
    }

    private SubmitExerciseCommand submission() {
        return new SubmitExerciseCommand(UUID.randomUUID(), List.of());
    }

    @Test
    void anAttemptWaitsWhileTheExaminerHasNotGradedItsEssay() {
        sendToExaminer.execute(user, attempt, question, UUID.randomUUID(), ESSAY, examiner);

        assertThatThrownBy(() -> submitAttempt.execute(user, attempt, submission()))
                .isInstanceOfSatisfying(LearningRequestException.class,
                        exception -> assertEquals("ESSAY_NOT_GRADED", exception.getCode()));
        assertEquals(0, count("SELECT count(*) FROM kp_evidence WHERE user_id = ?", user));
    }

    @Test
    void anAttemptSubmitsOnceTheExaminerHasGradedItsEssayAndCountsTheExaminersPass() {
        when(content.practiceSetAvailability(anyList(), anyList(), anyInt())).thenAnswer(invocation ->
                invocation.<List<UUID>>getArgument(0).stream().collect(java.util.stream.Collectors.toMap(p -> p, p -> 3)));
        var sent = sendToExaminer.execute(user, attempt, question, UUID.randomUUID(), ESSAY, examiner);
        applyResult.execute(examinerResult(sent.submissionId(), "6.5", "5.0"));

        var submitted = submitAttempt.execute(user, attempt, submission());

        assertTrue(submitted.passed(), "the examiner's mean 6.0 reaches the pass band 6.0");
        assertTrue(submitted.countedAsEvidence());
    }

    @Test
    void examinerFirstThenSubmitWritesNoCriterionEvidenceOrReviewTwice() {
        when(content.practiceSetAvailability(anyList(), anyList(), anyInt())).thenAnswer(invocation ->
                invocation.<List<UUID>>getArgument(0).stream().collect(java.util.stream.Collectors.toMap(p -> p, p -> 3)));
        var sent = sendToExaminer.execute(user, attempt, question, UUID.randomUUID(), ESSAY, examiner);
        applyResult.execute(examinerResult(sent.submissionId(), "6.5", "5.0"));

        submitAttempt.execute(user, attempt, submission());

        assertEquals(1, count("SELECT count(*) FROM kp_evidence WHERE user_id = ? AND kp_id = ? AND source = 'human_review'",
                user, coherence), "the examiner's criterion evidence stays the only one");
        assertEquals(0, count("SELECT count(*) FROM kp_evidence WHERE user_id = ? AND kp_id IN (?, ?) "
                + "AND source IN ('practice_set', 'lesson_writing')", user, taskResponse, coherence),
                "the submit writes no AI criterion evidence for the examiner's essay");
        assertEquals(1, count("SELECT count(*) FROM review_items WHERE user_id = ? AND trigger_kind = 'WRITING_CRITERION'",
                user), "the examiner's review is the only criterion review");
    }

    @Test
    void anAttemptWhoseExaminerMeanIsBelowThePassBandDoesNotPass() {
        var sent = sendToExaminer.execute(user, attempt, question, UUID.randomUUID(), ESSAY, examiner);
        applyResult.execute(examinerResult(sent.submissionId(), "5.0", "5.0"));

        var submitted = submitAttempt.execute(user, attempt, submission());

        assertEquals(false, submitted.passed(), "the examiner's mean 5.0 is below the pass band 6.0");
        assertTrue(submitted.countedAsEvidence());
    }
}
