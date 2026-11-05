package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.HumanReviewClient;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.usecase.HumanReviewUseCase;
import com.ieltspath.learning.application.usecase.ListExaminerReviewsUseCase;
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
    @MockitoBean HumanReviewClient client;
    @MockitoBean LearningContentClient content;

    private final UUID user = UUID.randomUUID();
    private final UUID examiner = UUID.randomUUID();

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE human_review_requests, lesson_writing_submissions, kp_evidence, review_items CASCADE");
        reset(client, content);
        when(content.getActiveRubric(any())).thenReturn(new LearningContentClient.Rubric("IELTS_WRITING", "TASK_2", 1,
                "source", List.of(), List.of()));
        when(client.consumeCredit(any())).thenReturn(new HumanReviewClient.CreditCharge(2, true));
    }

    private UUID essay(UUID owner, String status, boolean practice) {
        UUID id = UUID.randomUUID();
        UUID attempt = UUID.randomUUID();
        if (practice) {
            jdbc.update("INSERT INTO practice_attempts (id, user_id, lesson_id, skill, package_id, package_version_id) "
                    + "VALUES (?, ?, ?, 'WRITING', ?, ?)", attempt, owner, UUID.randomUUID(), UUID.randomUUID(),
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
}
