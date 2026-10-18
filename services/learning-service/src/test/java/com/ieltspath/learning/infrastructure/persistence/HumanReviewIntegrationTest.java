package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.HumanReviewClient;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.usecase.HumanReviewUseCase;
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
    @MockitoBean HumanReviewClient client;
    @MockitoBean LearningContentClient content;

    private final UUID user = UUID.randomUUID();

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

        assertEquals(review, reviews.request(user, essay));
        assertEquals(review, reviews.request(user, essay));

        verify(client, times(1)).consumeCredit(essay);
        var sent = org.mockito.ArgumentCaptor.forClass(HumanReviewClient.CreateReview.class);
        verify(client, times(1)).createReview(sent.capture());
        assertEquals("LESSON", sent.getValue().source());
        assertEquals("TASK_2", sent.getValue().task());
        assertEquals("My essay text", sent.getValue().essay());
        assertEquals(user, sent.getValue().learnerId());
        assertEquals(essay, sent.getValue().sourceSubmissionId());
        assertEquals("IELTS_WRITING", sent.getValue().rubricCode());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM human_review_requests", Integer.class));
    }

    @Test
    void aPracticeEssayIsSentAsPractice() {
        UUID essay = essay(user, "GRADED", true);
        when(client.createReview(any())).thenReturn(UUID.randomUUID());

        reviews.request(user, essay);

        var sent = org.mockito.ArgumentCaptor.forClass(HumanReviewClient.CreateReview.class);
        verify(client).createReview(sent.capture());
        assertEquals("PRACTICE", sent.getValue().source());
    }

    @Test
    void aReviewAssessmentRefusedGivesTheCreditBackAndTheLearnerCanAskAgain() {
        UUID essay = essay(user, "GRADED", false);
        when(client.createReview(any())).thenThrow(new LearningRequestException(409, "HUMAN_GRADING_CONFLICT", "x"));

        assertEquals(409, assertThrows(LearningRequestException.class, () -> reviews.request(user, essay)).getStatus());
        verify(client, times(1)).createReview(any());
        verify(client).refundCredit(essay);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM human_review_requests", Integer.class));

        reset(client);
        when(client.consumeCredit(any())).thenReturn(new HumanReviewClient.CreditCharge(1, true));
        UUID review = UUID.randomUUID();
        when(client.createReview(any())).thenReturn(review);
        assertEquals(review, reviews.request(user, essay));
    }

    @Test
    void aTimeoutKeepsTheCreditBecauseTheReviewMayExistAndTheRepeatReusesTheRequestId() {
        UUID essay = essay(user, "GRADED", false);
        UUID review = UUID.randomUUID();
        when(client.createReview(any())).thenThrow(new LearningRequestException(503, "HUMAN_REVIEW_UNAVAILABLE", "x"))
                .thenReturn(review);

        assertEquals(review, reviews.request(user, essay), "the repeat found the review the first call had made");

        var sent = org.mockito.ArgumentCaptor.forClass(HumanReviewClient.CreateReview.class);
        verify(client, times(2)).createReview(sent.capture());
        assertEquals(sent.getAllValues().get(0).requestId(), sent.getAllValues().get(1).requestId());
        verify(client, never()).refundCredit(any());
    }

    @Test
    void aLearnerWhoStillGetsNoAnswerKeepsTheCreditOnTheEssayAndPaysNothingMoreNextTime() {
        UUID essay = essay(user, "GRADED", false);
        when(client.createReview(any())).thenThrow(new LearningRequestException(503, "HUMAN_REVIEW_UNAVAILABLE", "x"));

        assertEquals(503, assertThrows(LearningRequestException.class, () -> reviews.request(user, essay)).getStatus());

        verify(client, times(2)).createReview(any());
        verify(client, never()).refundCredit(any());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM human_review_requests", Integer.class));
    }

    @Test
    void aRequestThatDidNotSpendTheCreditNeverGivesItBack() {
        UUID essay = essay(user, "GRADED", false);
        when(client.consumeCredit(any())).thenReturn(new HumanReviewClient.CreditCharge(2, false));
        when(client.createReview(any())).thenThrow(new LearningRequestException(409, "HUMAN_GRADING_CONFLICT", "x"));

        assertThrows(LearningRequestException.class, () -> reviews.request(user, essay));

        verify(client, never()).refundCredit(any());
    }

    @Test
    void aFailedRefundDoesNotHideTheOriginalFailure() {
        UUID essay = essay(user, "GRADED", false);
        when(client.createReview(any())).thenThrow(new LearningRequestException(409, "HUMAN_GRADING_CONFLICT", "x"));
        doThrow(new LearningRequestException(503, "HUMAN_REVIEW_UNAVAILABLE", "refund")).when(client).refundCredit(any());

        assertEquals("HUMAN_GRADING_CONFLICT",
                assertThrows(LearningRequestException.class, () -> reviews.request(user, essay)).getCode());
    }

    @Test
    void withoutACreditNothingIsSentAndTheLearnerIsToldSo() {
        UUID essay = essay(user, "GRADED", false);
        when(client.consumeCredit(any())).thenThrow(new LearningRequestException(402,
                "HUMAN_GRADING_CREDIT_REQUIRED", "none"));

        var refused = assertThrows(LearningRequestException.class, () -> reviews.request(user, essay));
        assertEquals(402, refused.getStatus());
        verify(client, never()).createReview(any());
        verify(client, never()).refundCredit(any());
    }

    @Test
    void onlyTheOwnersGradedEssayCanBeSent() {
        UUID notGraded = essay(user, "GRADING", false);
        UUID someoneElses = essay(UUID.randomUUID(), "GRADED", false);

        assertEquals("NOT_REVIEWABLE", assertThrows(LearningRequestException.class,
                () -> reviews.request(user, notGraded)).getCode());
        assertEquals(404, assertThrows(LearningRequestException.class,
                () -> reviews.request(user, someoneElses)).getStatus());
        assertEquals(404, assertThrows(LearningRequestException.class,
                () -> reviews.request(user, UUID.randomUUID())).getStatus());
        verifyNoInteractions(client);
    }

    @Test
    void theLearnerReadsTheReviewOfTheirOwnRequestOnly() {
        UUID essay = essay(user, "GRADED", false);
        UUID review = UUID.randomUUID();
        when(client.createReview(any())).thenReturn(review);
        var done = new HumanReviewClient.Review(review, "DONE", new BigDecimal("6.5"),
                List.of(new HumanReviewClient.Criterion("coherence", new BigDecimal("6.0"), "clear")),
                List.of("COHESION"), "Develop it");
        when(client.fetchReview(review, user)).thenReturn(done);

        assertEquals("NO_HUMAN_REVIEW", assertThrows(LearningRequestException.class,
                () -> reviews.review(user, essay)).getCode());
        reviews.request(user, essay);
        assertEquals(done, reviews.review(user, essay));
        assertEquals(404, assertThrows(LearningRequestException.class,
                () -> reviews.review(UUID.randomUUID(), essay)).getStatus());
    }

    @Test
    void aMentorReviewIsFeedbackOnlyAndWritesNoEvidenceOrReviews() {
        UUID essay = essay(user, "GRADED", false);
        when(client.createReview(any())).thenReturn(UUID.randomUUID());

        reviews.request(user, essay);

        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM kp_evidence", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM review_items", Integer.class));
        assertEquals("GRADED", jdbc.queryForObject("SELECT status FROM lesson_writing_submissions WHERE id = ?",
                String.class, essay));
    }
}
