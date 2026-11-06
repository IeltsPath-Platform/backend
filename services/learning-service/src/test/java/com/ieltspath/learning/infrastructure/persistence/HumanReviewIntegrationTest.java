package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.command.HumanReviewResult;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.HumanReviewClient;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.service.LessonEvidenceReference;
import com.ieltspath.learning.application.usecase.ApplyHumanReviewResultUseCase;
import com.ieltspath.learning.application.usecase.HumanReviewUseCase;
import com.ieltspath.learning.application.usecase.ListExaminerReviewsUseCase;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.vo.EvidenceSource;
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

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** A Mentor review of a graded essay: one credit per essay, refunded on failure, and feedback only. */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class HumanReviewIntegrationTest {
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
    @Autowired HumanReviewUseCase reviews;
    @Autowired ListExaminerReviewsUseCase list;
    @Autowired ApplyHumanReviewResultUseCase applyResult;
    @Autowired KnowledgeEvidenceRepository evidenceRepository;
    @MockitoBean HumanReviewClient client;
    @MockitoBean LearningContentClient content;

    private final UUID user = UUID.randomUUID();
    private final UUID examiner = UUID.randomUUID();

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE human_review_outcomes, human_review_requests, lesson_writing_submissions, kp_evidence, "
                + "review_items, knowledge_point_catalog CASCADE");
        reset(client, content);
        when(content.getActiveRubric(any())).thenReturn(new LearningContentClient.Rubric("IELTS_WRITING", "TASK_2", 1,
                "source", List.of(), List.of()));
        when(client.consumeCredit(any())).thenReturn(new HumanReviewClient.CreditCharge(2, true));
    }

    private UUID essay(UUID owner, String status, boolean practice) {
        UUID id = UUID.randomUUID();
        UUID attempt = UUID.randomUUID();
        if (practice) {
            jdbc.update("INSERT INTO practice_attempts (id, user_id, lesson_id, skill, skills, package_id, package_version_id) "
                    + "VALUES (?, ?, ?, 'WRITING', ARRAY['WRITING'], ?, ?)", attempt, owner, UUID.randomUUID(), UUID.randomUUID(),
                    UUID.randomUUID());
        }
        jdbc.update("""
                INSERT INTO lesson_writing_submissions (id, user_id, lesson_id, block_id, practice_attempt_id,
                    source, question_version_id, knowledge_point_ids, request_id, essay_text, word_count,
                    prompt_snapshot, status, point_cost, grading_started_at, submitted_at, result, overall_band,
                    passed, debit_ledger_entry_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ARRAY[]::uuid[], ?, 'My essay text', 3,
                    CAST(? AS jsonb), ?, 3, now(), now(), CAST(? AS jsonb), ?, ?, ?)
                """, id, owner, UUID.randomUUID(), practice ? null : UUID.randomUUID(), practice ? attempt : null,
                practice ? "PRACTICE_ITEM" : "LESSON_BLOCK", UUID.randomUUID(), UUID.randomUUID(),
                "{\"stem\":\"Should cities plant trees?\",\"task\":\"TASK_2\"}", status,
                "GRADED".equals(status) ? "{\"criteria\":[],\"corrections\":[],\"summary\":\"ok\"}" : null,
                "GRADED".equals(status) ? new BigDecimal("6.0") : null, "GRADED".equals(status) ? true : null,
                "GRADED".equals(status) ? UUID.randomUUID() : null);
        return id;
    }

    /** A catalog point as Learning keeps it: a grammar or writing point, or an error trap under its parent. */
    private void catalogPoint(UUID point, UUID parent, String errorCode) {
        jdbc.update("""
                INSERT INTO knowledge_point_catalog (kp_id, topic_id, has_practice_set, skill, parent_kp_id, error_code)
                VALUES (?, ?, ?, 'WRITING', ?, ?)
                """, point, UUID.randomUUID(), parent == null, parent, errorCode);
    }

    private int count(String sql, Object... arguments) {
        return jdbc.queryForObject(sql, Integer.class, arguments);
    }

    private static HumanReviewResult.Criterion criterion(String name, String band) {
        return new HumanReviewResult.Criterion(name, new BigDecimal(band));
    }

    /**
     * The examiner's criterion results take the place of the AI's on the same essay, the marked error counts as one wrong
     * answer on its trap point, and a repeated event changes nothing. Evidence from other sources is never touched.
     */
    @Test
    void anExaminersResultReplacesTheAiEvidenceOfTheEssayAndIsAppliedOnce() {
        UUID essay = essay(user, "GRADED", false);
        UUID review = UUID.randomUUID();
        jdbc.update("UPDATE lesson_writing_submissions SET prompt_snapshot = jsonb_set(prompt_snapshot, '{passBand}', '6.0') "
                + "WHERE id = ?", essay);
        jdbc.update("INSERT INTO human_review_requests (submission_id, user_id, review_submission_id) VALUES (?, ?, ?)",
                essay, user, review);
        UUID coherence = UUID.randomUUID(), taskResponse = UUID.randomUUID(), parent = UUID.randomUUID(),
                trap = UUID.randomUUID();
        catalogPoint(coherence, null, null);
        catalogPoint(taskResponse, null, null);
        catalogPoint(parent, null, null);
        catalogPoint(trap, parent, "LINKER_MISUSE");
        when(content.getActiveRubric("TASK_2")).thenReturn(new LearningContentClient.Rubric("IELTS_WRITING_TASK_2", "TASK_2",
                1, "source", List.of(
                new LearningContentClient.RubricCriterion("TR", "Task response", "", 1, List.of(), taskResponse),
                new LearningContentClient.RubricCriterion("CC", "Coherence", "", 2, List.of(), coherence)), List.of()));
        when(content.activeErrorTypes()).thenReturn(List.of(
                new LearningContentClient.ErrorType("LINKER_MISUSE", "Linker", "COHERENCE", 1, "Use the right linker")));
        // The AI judged coherence right on this essay; a review by someone else must survive the replacement.
        evidenceRepository.append(user, List.of(
                KnowledgeEvidence.of(coherence, true, EvidenceSource.LESSON_WRITING, LessonEvidenceReference.forWriting(essay, coherence)),
                KnowledgeEvidence.of(coherence, true, EvidenceSource.REVIEW_SET, LessonEvidenceReference.forWriting(essay, coherence))));
        HumanReviewResult result = new HumanReviewResult(UUID.randomUUID(), user, review, essay,
                List.of(criterion("coherence", "5.0"), criterion("taskResponse", "6.5")),
                List.of(new HumanReviewResult.ErrorCount("LINKER_MISUSE", 1)));

        applyResult.execute(result);

        assertEquals(0, count("SELECT count(*) FROM kp_evidence WHERE source = 'lesson_writing' AND kp_id = ?", coherence),
                "the AI's criterion evidence for this essay is replaced");
        assertEquals(1, count("SELECT count(*) FROM kp_evidence WHERE source = 'review_set' AND kp_id = ?", coherence),
                "evidence of another source is kept");
        assertEquals(false, jdbc.queryForObject("SELECT correct FROM kp_evidence WHERE source = 'human_review' AND kp_id = ?",
                Boolean.class, coherence));
        assertEquals(true, jdbc.queryForObject("SELECT correct FROM kp_evidence WHERE source = 'human_review' AND kp_id = ?",
                Boolean.class, taskResponse));
        assertEquals(1, count("SELECT count(*) FROM kp_evidence WHERE source = 'error_trap' AND kp_id = ?", trap));
        assertEquals(1, count("SELECT count(*) FROM human_review_outcomes WHERE submission_id = ?", essay));

        applyResult.execute(result);

        assertEquals(2, count("SELECT count(*) FROM kp_evidence WHERE source = 'human_review'"),
                "a repeated event adds no evidence");
        assertEquals(1, count("SELECT count(*) FROM kp_evidence WHERE source = 'error_trap'"));
        assertEquals(1, count("SELECT count(*) FROM human_review_outcomes"));
    }

    /**
     * The first weak grade of a criterion leaves an optional review; the same criterion found weak again while that review
     * waits makes it blocking, and a criterion that was not weak before gets no blocking review.
     */
    @Test
    void aCriterionFoundWeakAgainWhileItsReviewWaitsBecomesARequiredReview() {
        UUID coherence = UUID.randomUUID(), taskResponse = UUID.randomUUID();
        catalogPoint(coherence, null, null);
        catalogPoint(taskResponse, null, null);
        when(content.getActiveRubric("TASK_2")).thenReturn(new LearningContentClient.Rubric("IELTS_WRITING_TASK_2", "TASK_2",
                1, "source", List.of(
                new LearningContentClient.RubricCriterion("TR", "Task response", "", 1, List.of(), taskResponse),
                new LearningContentClient.RubricCriterion("CC", "Coherence", "", 2, List.of(), coherence)), List.of()));
        when(content.activeErrorTypes()).thenReturn(List.of());
        when(content.practiceSetAvailability(anyList(), anyList(), anyInt())).thenAnswer(invocation ->
                invocation.<List<UUID>>getArgument(0).stream().collect(java.util.stream.Collectors.toMap(p -> p, p -> 3)));

        gradeWeak(coherence, "4.0", "6.5");
        assertEquals(1, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND trigger_kind = 'WRITING_CRITERION' "
                + "AND blocking = false AND status = 'PENDING'", coherence), "the first weak grade is a suggestion");
        assertEquals(0, count("SELECT count(*) FROM review_items WHERE blocking = true"));

        gradeWeak(coherence, "4.5", "6.5");

        assertEquals(1, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND trigger_kind = 'WRITING_CRITERION' "
                + "AND blocking = true AND status = 'PENDING'", coherence), "weak again: the same review is now required");
        assertEquals(1, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ?", coherence), "no second review");
        assertEquals(0, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND blocking = true", taskResponse));
    }

    /** The examiner grades this essay: a coherence band and a task response band. */
    private void gradeEssay(UUID essay, String coherenceBand, String taskBand) {
        UUID review = UUID.randomUUID();
        jdbc.update("UPDATE lesson_writing_submissions SET prompt_snapshot = jsonb_set(prompt_snapshot, '{passBand}', '6.0') "
                + "WHERE id = ?", essay);
        jdbc.update("INSERT INTO human_review_requests (submission_id, user_id, review_submission_id) VALUES (?, ?, ?)",
                essay, user, review);
        applyResult.execute(new HumanReviewResult(UUID.randomUUID(), user, review, essay,
                List.of(criterion("coherence", coherenceBand), criterion("taskResponse", taskBand)), List.of()));
    }

    /** One graded course essay of the learner, with the examiner's coherence band; {@code coherence} is the KP judged. */
    private void gradeWeak(UUID coherence, String coherenceBand, String taskBand) {
        gradeEssay(essay(user, "GRADED", true), coherenceBand, taskBand);
    }

    /** Points and rubric for the coherence and task response criteria, as the examiner's grade reads them. */
    private void stageCriteria(UUID coherence, UUID taskResponse) {
        catalogPoint(coherence, null, null);
        catalogPoint(taskResponse, null, null);
        when(content.getActiveRubric("TASK_2")).thenReturn(new LearningContentClient.Rubric("IELTS_WRITING_TASK_2", "TASK_2",
                1, "source", List.of(
                new LearningContentClient.RubricCriterion("TR", "Task response", "", 1, List.of(), taskResponse),
                new LearningContentClient.RubricCriterion("CC", "Coherence", "", 2, List.of(), coherence)), List.of()));
        when(content.activeErrorTypes()).thenReturn(List.of());
        when(content.practiceSetAvailability(anyList(), anyList(), anyInt())).thenAnswer(invocation ->
                invocation.<List<UUID>>getArgument(0).stream().collect(java.util.stream.Collectors.toMap(p -> p, p -> 3)));
    }

    /** A criterion review already on the learner's list, raised by {@code essay} (null when no essay did). */
    private void existingReview(UUID point, UUID essay, String trigger, String skill, String status, boolean blocking) {
        UUID lesson = essay == null ? UUID.randomUUID()
                : jdbc.queryForObject("SELECT lesson_id FROM lesson_writing_submissions WHERE id = ?", UUID.class, essay);
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, trigger_kind, "
                + "blocking, source_submission_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), user, point, lesson, status, skill, trigger, blocking, essay);
    }

    /** An error code Content has not approved is neither counted nor kept for the learner. */
    @Test
    void anUnapprovedCodeLeavesNoTrapEvidenceAndNoStudyPoint() {
        UUID essay = essay(user, "GRADED", false);
        UUID review = UUID.randomUUID();
        jdbc.update("UPDATE lesson_writing_submissions SET prompt_snapshot = jsonb_set(prompt_snapshot, '{passBand}', '6.0') "
                + "WHERE id = ?", essay);
        jdbc.update("INSERT INTO human_review_requests (submission_id, user_id, review_submission_id) VALUES (?, ?, ?)",
                essay, user, review);
        UUID parent = UUID.randomUUID(), trap = UUID.randomUUID();
        catalogPoint(parent, null, null);
        catalogPoint(trap, parent, "LINKER_MISUSE");
        when(content.activeErrorTypes()).thenReturn(List.of());

        applyResult.execute(new HumanReviewResult(UUID.randomUUID(), user, review, essay, List.of(),
                List.of(new HumanReviewResult.ErrorCount("LINKER_MISUSE", 2))));

        assertEquals(0, count("SELECT count(*) FROM kp_evidence WHERE source = 'error_trap'"));
        assertEquals("[]", jdbc.queryForObject("SELECT study_points::text FROM human_review_outcomes WHERE submission_id = ?",
                String.class, essay));
    }

    @Test
    void aGradedLessonEssayIsSentOnceAndAskingAgainSpendsNothing() {
        UUID essay = essay(user, "GRADED", false);
        UUID review = UUID.randomUUID();
        when(client.createReview(any())).thenReturn(review);

        assertEquals(review, reviews.request(user, essay, examiner));
        assertEquals(review, reviews.request(user, essay, examiner));

        verify(client, times(1)).consumeCredit(essay);
        var sent = org.mockito.ArgumentCaptor.forClass(HumanReviewClient.CreateReview.class);
        verify(client, times(1)).createReview(sent.capture());
        assertEquals(examiner, sent.getValue().examinerId());
        assertEquals("LESSON", sent.getValue().source());
        assertEquals("TASK_2", sent.getValue().task());
        assertEquals("My essay text", sent.getValue().essay());
        assertEquals(user, sent.getValue().learnerId());
        assertEquals(essay, sent.getValue().sourceSubmissionId());
        assertEquals("IELTS_WRITING", sent.getValue().rubricCode());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM human_review_requests", Integer.class));
    }

    @Test
    void theLearnerListsTheirOwnRequestsNewestFirstOnePageAtATime() {
        UUID older = essay(user, "GRADED", false);
        UUID newer = essay(user, "GRADED", true);
        UUID someoneElses = essay(UUID.randomUUID(), "GRADED", false);
        UUID olderReview = UUID.randomUUID(), newerReview = UUID.randomUUID();
        jdbc.update("INSERT INTO human_review_requests (submission_id, user_id, review_submission_id, requested_at) VALUES (?, ?, ?, now() - interval '2 days')", older, user, olderReview);
        jdbc.update("INSERT INTO human_review_requests (submission_id, user_id, review_submission_id, requested_at) VALUES (?, ?, ?, now() - interval '1 day')", newer, user, newerReview);
        jdbc.update("INSERT INTO human_review_requests (submission_id, user_id, review_submission_id) VALUES (?, ?, ?)", someoneElses, UUID.randomUUID(), UUID.randomUUID());

        var first = list.execute(user, 0, 1);
        assertEquals(1, first.items().size());
        assertTrue(first.hasNext());
        var row = first.items().getFirst();
        assertEquals(newer, row.submissionId());
        assertEquals(newerReview, row.reviewSubmissionId());
        assertEquals("PRACTICE_ITEM", row.source());
        assertEquals("TASK_2", row.task());
        assertEquals("Should cities plant trees?", row.stem());
        assertEquals(3, row.wordCount());

        var second = list.execute(user, 1, 1);
        assertEquals(older, second.items().getFirst().submissionId());
        assertFalse(second.hasNext());
        assertEquals(0, list.execute(UUID.randomUUID(), 0, 20).items().size(), "nobody sees another learner's essays");
    }

    @Test
    void aPracticeEssayIsSentAsPractice() {
        UUID essay = essay(user, "GRADED", true);
        when(client.createReview(any())).thenReturn(UUID.randomUUID());

        reviews.request(user, essay, examiner);

        var sent = org.mockito.ArgumentCaptor.forClass(HumanReviewClient.CreateReview.class);
        verify(client).createReview(sent.capture());
        assertEquals("PRACTICE", sent.getValue().source());
    }

    @Test
    void aReviewAssessmentRefusedGivesTheCreditBackAndTheLearnerCanAskAgain() {
        UUID essay = essay(user, "GRADED", false);
        when(client.createReview(any())).thenThrow(new LearningRequestException(409, "HUMAN_GRADING_CONFLICT", "x"));

        assertEquals(409, assertThrows(LearningRequestException.class, () -> reviews.request(user, essay, examiner)).getStatus());
        verify(client, times(1)).createReview(any());
        verify(client).refundCredit(essay);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM human_review_requests", Integer.class));

        reset(client);
        when(client.consumeCredit(any())).thenReturn(new HumanReviewClient.CreditCharge(1, true));
        UUID review = UUID.randomUUID();
        when(client.createReview(any())).thenReturn(review);
        assertEquals(review, reviews.request(user, essay, examiner));
    }

    @Test
    void anExaminerWhoCannotTakeTheEssayGivesTheCreditBackAndTheLearnerChoosesAnother() {
        UUID essay = essay(user, "GRADED", false);
        UUID other = UUID.randomUUID();
        UUID review = UUID.randomUUID();
        when(client.createReview(any())).thenThrow(new LearningRequestException(409, "EXAMINER_UNAVAILABLE", "full"));

        assertEquals("EXAMINER_UNAVAILABLE", assertThrows(LearningRequestException.class,
                () -> reviews.request(user, essay, examiner)).getCode());
        verify(client, times(1)).createReview(any());
        verify(client).refundCredit(essay);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM human_review_requests", Integer.class));

        reset(client);
        when(client.consumeCredit(any())).thenReturn(new HumanReviewClient.CreditCharge(1, true));
        when(client.createReview(any())).thenReturn(review);
        assertEquals(review, reviews.request(user, essay, other));
        var sent = org.mockito.ArgumentCaptor.forClass(HumanReviewClient.CreateReview.class);
        verify(client).createReview(sent.capture());
        assertEquals(other, sent.getValue().examinerId());
    }

    @Test
    void aRequestWithoutAnExaminerSpendsNothing() {
        UUID essay = essay(user, "GRADED", false);

        assertEquals("EXAMINER_REQUIRED", assertThrows(LearningRequestException.class,
                () -> reviews.request(user, essay, null)).getCode());
        verifyNoInteractions(client);
    }

    @Test
    void aTimeoutKeepsTheCreditBecauseTheReviewMayExistAndTheRepeatReusesTheRequestId() {
        UUID essay = essay(user, "GRADED", false);
        UUID review = UUID.randomUUID();
        when(client.createReview(any())).thenThrow(new LearningRequestException(503, "HUMAN_REVIEW_UNAVAILABLE", "x"))
                .thenReturn(review);

        assertEquals(review, reviews.request(user, essay, examiner), "the repeat found the review the first call had made");

        var sent = org.mockito.ArgumentCaptor.forClass(HumanReviewClient.CreateReview.class);
        verify(client, times(2)).createReview(sent.capture());
        assertEquals(sent.getAllValues().get(0).requestId(), sent.getAllValues().get(1).requestId());
        verify(client, never()).refundCredit(any());
    }

    @Test
    void aLearnerWhoStillGetsNoAnswerKeepsTheCreditOnTheEssayAndPaysNothingMoreNextTime() {
        UUID essay = essay(user, "GRADED", false);
        when(client.createReview(any())).thenThrow(new LearningRequestException(503, "HUMAN_REVIEW_UNAVAILABLE", "x"));

        assertEquals(503, assertThrows(LearningRequestException.class, () -> reviews.request(user, essay, examiner)).getStatus());

        verify(client, times(2)).createReview(any());
        verify(client, never()).refundCredit(any());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM human_review_requests", Integer.class));
    }

    @Test
    void aRequestThatDidNotSpendTheCreditNeverGivesItBack() {
        UUID essay = essay(user, "GRADED", false);
        when(client.consumeCredit(any())).thenReturn(new HumanReviewClient.CreditCharge(2, false));
        when(client.createReview(any())).thenThrow(new LearningRequestException(409, "HUMAN_GRADING_CONFLICT", "x"));

        assertThrows(LearningRequestException.class, () -> reviews.request(user, essay, examiner));

        verify(client, never()).refundCredit(any());
    }

    @Test
    void aFailedRefundDoesNotHideTheOriginalFailure() {
        UUID essay = essay(user, "GRADED", false);
        when(client.createReview(any())).thenThrow(new LearningRequestException(409, "HUMAN_GRADING_CONFLICT", "x"));
        doThrow(new LearningRequestException(503, "HUMAN_REVIEW_UNAVAILABLE", "refund")).when(client).refundCredit(any());

        assertEquals("HUMAN_GRADING_CONFLICT",
                assertThrows(LearningRequestException.class, () -> reviews.request(user, essay, examiner)).getCode());
    }

    @Test
    void withoutACreditNothingIsSentAndTheLearnerIsToldSo() {
        UUID essay = essay(user, "GRADED", false);
        when(client.consumeCredit(any())).thenThrow(new LearningRequestException(402,
                "HUMAN_GRADING_CREDIT_REQUIRED", "none"));

        var refused = assertThrows(LearningRequestException.class, () -> reviews.request(user, essay, examiner));
        assertEquals(402, refused.getStatus());
        verify(client, never()).createReview(any());
        verify(client, never()).refundCredit(any());
    }

    @Test
    void onlyTheOwnersGradedEssayCanBeSent() {
        UUID notGraded = essay(user, "GRADING", false);
        UUID someoneElses = essay(UUID.randomUUID(), "GRADED", false);

        assertEquals("NOT_REVIEWABLE", assertThrows(LearningRequestException.class,
                () -> reviews.request(user, notGraded, examiner)).getCode());
        assertEquals(404, assertThrows(LearningRequestException.class,
                () -> reviews.request(user, someoneElses, examiner)).getStatus());
        assertEquals(404, assertThrows(LearningRequestException.class,
                () -> reviews.request(user, UUID.randomUUID(), examiner)).getStatus());
        verifyNoInteractions(client);
    }

    @Test
    void theLearnerReadsTheReviewOfTheirOwnRequestOnly() {
        UUID essay = essay(user, "GRADED", false);
        UUID review = UUID.randomUUID();
        when(client.createReview(any())).thenReturn(review);
        var done = new HumanReviewClient.Review(review, "DONE", new BigDecimal("6.5"),
                List.of(new HumanReviewClient.Criterion("coherence", new BigDecimal("6.0"), "clear")),
                List.of("COHESION"), "Develop it",
                List.of(new HumanReviewClient.Annotation("independent practice", 3, 23, null, "practised alone")),
                examiner);
        when(client.fetchReview(review, user)).thenReturn(done);

        assertEquals("NO_HUMAN_REVIEW", assertThrows(LearningRequestException.class,
                () -> reviews.review(user, essay)).getCode());
        reviews.request(user, essay, examiner);
        assertEquals(done, reviews.review(user, essay));
        assertEquals(404, assertThrows(LearningRequestException.class,
                () -> reviews.review(UUID.randomUUID(), essay)).getStatus());
    }

    @Test
    void cancellingALateRequestClosesItInAssessmentThenGivesTheCreditBack() {
        UUID essay = essay(user, "GRADED", false);
        UUID review = UUID.randomUUID();
        when(client.createReview(any())).thenReturn(review);
        reviews.request(user, essay, examiner);
        var cancelled = new HumanReviewClient.Review(review, "CANCELLED", null, null, null, null, null, examiner);
        when(client.cancelReview(review)).thenReturn(cancelled);

        assertEquals(cancelled, reviews.cancel(user, review));

        var order = org.mockito.Mockito.inOrder(client);
        order.verify(client).cancelReview(review);
        order.verify(client).refundCredit(essay);
    }

    @Test
    void aRequestThatCannotBeCancelledYetKeepsItsCredit() {
        UUID essay = essay(user, "GRADED", false);
        UUID review = UUID.randomUUID();
        when(client.createReview(any())).thenReturn(review);
        reviews.request(user, essay, examiner);
        when(client.cancelReview(review)).thenThrow(new LearningRequestException(409, "REVIEW_CANCEL_NOT_ALLOWED", "x"));

        assertEquals("REVIEW_CANCEL_NOT_ALLOWED", assertThrows(LearningRequestException.class, () -> reviews.cancel(user, review)).getCode());
        verify(client, never()).refundCredit(any());
    }

    @Test
    void afterAFailedRefundTheLearnerCancelsAgainAndTheCreditComesBack() {
        UUID essay = essay(user, "GRADED", false);
        UUID review = UUID.randomUUID();
        when(client.createReview(any())).thenReturn(review);
        reviews.request(user, essay, examiner);
        when(client.cancelReview(review)).thenReturn(new HumanReviewClient.Review(review, "CANCELLED", null, null, null, null, null, examiner));
        org.mockito.Mockito.doThrow(new LearningRequestException(503, "HUMAN_REVIEW_UNAVAILABLE", "x"))
                .doNothing().when(client).refundCredit(essay);

        assertEquals(503, assertThrows(LearningRequestException.class, () -> reviews.cancel(user, review)).getStatus());
        assertEquals("CANCELLED", reviews.cancel(user, review).status());
        verify(client, times(2)).refundCredit(essay);
    }

    @Test
    void onlyTheOwnerCancelsAndACancelledRequestIsNotAskedAgain() {
        UUID essay = essay(user, "GRADED", false);
        UUID review = UUID.randomUUID();
        when(client.createReview(any())).thenReturn(review);
        reviews.request(user, essay, examiner);

        assertEquals(404, assertThrows(LearningRequestException.class, () -> reviews.cancel(UUID.randomUUID(), review)).getStatus());
        assertEquals(404, assertThrows(LearningRequestException.class, () -> reviews.cancel(user, UUID.randomUUID())).getStatus());
        verify(client, never()).cancelReview(any());
        verify(client, never()).refundCredit(any());

        when(client.fetchReview(review, user)).thenReturn(new HumanReviewClient.Review(review, "CANCELLED", null, null, null, null, null, examiner));
        assertEquals("REVIEW_CANCELLED", assertThrows(LearningRequestException.class, () -> reviews.request(user, essay, examiner)).getCode());
        verify(client, times(1)).consumeCredit(essay);
    }

    @Test
    void aMentorReviewIsFeedbackOnlyAndWritesNoEvidenceOrReviews() {
        UUID essay = essay(user, "GRADED", false);
        when(client.createReview(any())).thenReturn(UUID.randomUUID());

        reviews.request(user, essay, examiner);

        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM kp_evidence", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM review_items", Integer.class));
        assertEquals("GRADED", jdbc.queryForObject("SELECT status FROM lesson_writing_submissions WHERE id = ?",
                String.class, essay));
    }

    @Test
    void theExaminerGradeRecordsItsEssayOnTheOptionalReviewItRaises() {
        UUID coherence = UUID.randomUUID(), taskResponse = UUID.randomUUID();
        stageCriteria(coherence, taskResponse);
        UUID essay = essay(user, "GRADED", true);

        gradeEssay(essay, "4.5", "6.5");

        assertEquals(essay, jdbc.queryForObject("SELECT source_submission_id FROM review_items WHERE knowledge_point_id = ?",
                UUID.class, coherence));
        assertEquals(1, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND blocking = false "
                + "AND status = 'PENDING'", coherence));
    }

    @Test
    void anOptionalReviewFromAnotherEssayBlocksWhenTheExaminerFindsTheCriterionWeakAgain() {
        UUID coherence = UUID.randomUUID(), taskResponse = UUID.randomUUID();
        stageCriteria(coherence, taskResponse);
        existingReview(coherence, essay(user, "GRADED", true), "WRITING_CRITERION", "WRITING", "PENDING", false);

        gradeEssay(essay(user, "GRADED", true), "4.5", "6.5");

        assertEquals(1, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND blocking = true "
                + "AND status = 'PENDING' AND trigger_kind = 'WRITING_CRITERION'", coherence));
        assertEquals(1, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ?", coherence),
                "the same review is promoted, no second one is raised");
    }

    @Test
    void anOptionalReviewOfTheSameEssayIsNotASecondGradeAndStaysOptional() {
        UUID coherence = UUID.randomUUID(), taskResponse = UUID.randomUUID();
        stageCriteria(coherence, taskResponse);
        UUID essay = essay(user, "GRADED", true);
        existingReview(coherence, essay, "WRITING_CRITERION", "WRITING", "PENDING", false);

        gradeEssay(essay, "4.5", "6.5");

        assertEquals(0, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND blocking = true", coherence));
        assertEquals(1, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND blocking = false "
                + "AND status = 'PENDING'", coherence));
    }

    @Test
    void anOptionalReviewFromAnotherEssayIsClosedWhenTheExaminerPassesTheCriterion() {
        UUID coherence = UUID.randomUUID(), taskResponse = UUID.randomUUID();
        stageCriteria(coherence, taskResponse);
        existingReview(coherence, essay(user, "GRADED", true), "WRITING_CRITERION", "WRITING", "PENDING", false);

        gradeEssay(essay(user, "GRADED", true), "6.5", "6.5");

        assertEquals(1, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND status = 'DONE' "
                + "AND done_at IS NOT NULL", coherence));
        assertEquals(0, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND status = 'PENDING'", coherence));
    }

    @Test
    void aBlockingReviewFromAnotherEssayStaysBlockingWithoutADuplicate() {
        UUID coherence = UUID.randomUUID(), taskResponse = UUID.randomUUID();
        stageCriteria(coherence, taskResponse);
        existingReview(coherence, essay(user, "GRADED", true), "WRITING_CRITERION", "WRITING", "PENDING", true);

        gradeEssay(essay(user, "GRADED", true), "4.5", "6.5");

        assertEquals(1, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND blocking = true "
                + "AND status = 'PENDING'", coherence));
        assertEquals(1, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ?", coherence));
    }

    @Test
    void aSpeakingCriterionReviewOfTheSamePointIsNeverEscalatedByTheExaminer() {
        UUID coherence = UUID.randomUUID(), taskResponse = UUID.randomUUID();
        stageCriteria(coherence, taskResponse);
        existingReview(coherence, null, "SPEAKING_CRITERION", "SPEAKING", "PENDING", false);

        gradeEssay(essay(user, "GRADED", true), "4.5", "6.5");

        assertEquals(0, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND blocking = true", coherence));
        assertEquals(1, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND trigger_kind = 'SPEAKING_CRITERION' "
                + "AND blocking = false AND status = 'PENDING'", coherence));
    }

    @Test
    void aReviewWithNoRecordedEssayCountsAsAnotherEssay() {
        UUID coherence = UUID.randomUUID(), taskResponse = UUID.randomUUID();
        stageCriteria(coherence, taskResponse);
        existingReview(coherence, null, "WRITING_CRITERION", "WRITING", "PENDING", false);

        gradeEssay(essay(user, "GRADED", true), "4.5", "6.5");

        assertEquals(1, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND blocking = true "
                + "AND status = 'PENDING'", coherence));
    }

    @Test
    void aFinishedReviewIsNotEscalatedAndTheExaminerRaisesANewOptionalOne() {
        UUID coherence = UUID.randomUUID(), taskResponse = UUID.randomUUID();
        stageCriteria(coherence, taskResponse);
        existingReview(coherence, essay(user, "GRADED", true), "WRITING_CRITERION", "WRITING", "DONE", false);
        UUID essay = essay(user, "GRADED", true);

        gradeEssay(essay, "4.5", "6.5");

        assertEquals(0, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND blocking = true "
                + "AND status = 'PENDING'", coherence));
        assertEquals(1, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND blocking = false "
                + "AND status = 'PENDING' AND source_submission_id = ?", coherence, essay));
    }

    @Test
    void anOptionalReviewOfTheSameEssayIsClosedWhenTheExaminerPassesTheCriterion() {
        UUID coherence = UUID.randomUUID(), taskResponse = UUID.randomUUID();
        stageCriteria(coherence, taskResponse);
        UUID essay = essay(user, "GRADED", true);
        existingReview(coherence, essay, "WRITING_CRITERION", "WRITING", "PENDING", false);

        gradeEssay(essay, "6.5", "6.5");

        assertEquals(1, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND status = 'DONE'", coherence));
    }

    @Test
    void aBlockingReviewIsNotClosedByAPassingGrade() {
        UUID coherence = UUID.randomUUID(), taskResponse = UUID.randomUUID();
        stageCriteria(coherence, taskResponse);
        existingReview(coherence, essay(user, "GRADED", true), "WRITING_CRITERION", "WRITING", "PENDING", true);

        gradeEssay(essay(user, "GRADED", true), "6.5", "6.5");

        assertEquals(1, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND status = 'PENDING' "
                + "AND blocking = true", coherence));
    }

    @Test
    void aSpeakingOrPracticeReviewOfThePointIsNotClosedByAPassingGrade() {
        UUID coherence = UUID.randomUUID(), taskResponse = UUID.randomUUID();
        stageCriteria(coherence, taskResponse);
        existingReview(coherence, null, "SPEAKING_CRITERION", "SPEAKING", "PENDING", false);
        existingReview(taskResponse, null, "PRACTICE", "WRITING", "PENDING", false);

        gradeEssay(essay(user, "GRADED", true), "6.5", "6.5");

        assertEquals(1, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND status = 'PENDING' "
                + "AND trigger_kind = 'SPEAKING_CRITERION'", coherence));
        assertEquals(1, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND status = 'PENDING' "
                + "AND trigger_kind = 'PRACTICE'", taskResponse));
    }

    @Test
    void aReviewOfAPointTheGradeDoesNotJudgeIsNotClosed() {
        UUID coherence = UUID.randomUUID(), taskResponse = UUID.randomUUID(), other = UUID.randomUUID();
        stageCriteria(coherence, taskResponse);
        existingReview(other, essay(user, "GRADED", true), "WRITING_CRITERION", "WRITING", "PENDING", false);

        gradeEssay(essay(user, "GRADED", true), "6.5", "6.5");

        assertEquals(1, count("SELECT count(*) FROM review_items WHERE knowledge_point_id = ? AND status = 'PENDING'", other));
    }

    @Test
    void aPassingGradeWithNoWaitingReviewCreatesNothing() {
        UUID coherence = UUID.randomUUID(), taskResponse = UUID.randomUUID();
        stageCriteria(coherence, taskResponse);

        gradeEssay(essay(user, "GRADED", true), "6.5", "6.5");

        assertEquals(0, count("SELECT count(*) FROM review_items"));
    }
}
