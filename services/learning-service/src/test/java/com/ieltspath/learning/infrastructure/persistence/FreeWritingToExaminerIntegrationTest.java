package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.HumanReviewClient;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Item;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.port.LearningContentClient.Section;
import com.ieltspath.learning.application.port.LlmClient;
import com.ieltspath.learning.application.usecase.GetWritingSubmissionUseCase;
import com.ieltspath.learning.application.usecase.SubmitFreeWritingToExaminerUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A free-writing essay sent to an examiner instead of the AI: it waits for the examiner, costs no points, needs a Premium
 * grading credit, and is stored once however many times it is sent.
 */
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
class FreeWritingToExaminerIntegrationTest {
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
    private static final UUID OTHER_USER = UUID.randomUUID();
    private static final UUID EXAMINER = UUID.randomUUID();
    private static final UUID OTHER_EXAMINER = UUID.randomUUID();
    private static final UUID VERSION = UUID.randomUUID();
    private static final UUID LESSON_VERSION = UUID.randomUUID();
    private static final UUID ESSAY = UUID.randomUUID();
    private static final UUID OBJECTIVE = UUID.randomUUID();
    private static final String TEXT = ("The line graph shows that mobile phones became the most common device while "
            + "tablets appeared only after 2010. ").repeat(5);

    @Autowired JdbcTemplate jdbc;
    @Autowired SubmitFreeWritingToExaminerUseCase sendToExaminer;
    @Autowired GetWritingSubmissionUseCase getSubmission;
    @MockitoBean LearningContentClient content;
    @MockitoBean AccessClient access;
    @MockitoBean LlmClient llm;
    @MockitoBean HumanReviewClient client;
    @MockitoBean com.ieltspath.learning.application.port.EssayPromptActivationProvider promptActivations;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE human_review_requests, lesson_writing_submissions, llm_daily_usage CASCADE");
        reset(content, access, llm, client);
        var section = new Section(UUID.randomUUID(), "Task 1", "WRITING", null, 1, null, List.of(
                new Item(ESSAY, 1, "The line graph below shows device ownership.", null,
                        Map.of("type", "ESSAY", "task", "TASK_1", "minWords", 150, "passBand", 5.5), "Model essay",
                        BigDecimal.ONE, List.of()),
                new Item(OBJECTIVE, 2, "Pick", null, Map.of("type", "CHOICE", "correct", "A"), "Because",
                        BigDecimal.ONE, List.of())));
        when(content.getPackageVersion(VERSION)).thenReturn(new PackageVersion(VERSION, UUID.randomUUID(),
                "PRACTICE_TEST", null, Map.of(), List.of(section)));
        when(content.getPackageVersion(LESSON_VERSION)).thenReturn(new PackageVersion(LESSON_VERSION, UUID.randomUUID(),
                "COURSE_TEST", null, Map.of(), List.of(section)));
        when(content.getActiveRubric(anyString())).thenReturn(new LearningContentClient.Rubric("IELTS_WRITING", "TASK_1",
                1, "source", List.of(), List.of()));
        when(client.consumeCredit(any())).thenReturn(new HumanReviewClient.CreditCharge(3, true));
        when(client.createReview(any())).thenAnswer(invocation -> UUID.randomUUID());
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private void noAiAndNoPoints() {
        verifyNoInteractions(llm);
        verify(access, never()).balance();
        verify(access, never()).debit(any(), org.mockito.ArgumentMatchers.anyInt(), any(), anyString(), any());
    }

    @Test
    void anEssaySentToAnExaminerWaitsForThemWithoutTheAiOrPoints() {
        UUID request = UUID.randomUUID();

        var sent = sendToExaminer.execute(USER, VERSION, ESSAY, request, TEXT, EXAMINER);

        assertEquals("AWAITING_EXAMINER", sent.status());
        assertEquals("TASK_1", sent.task());
        assertEquals(TEXT, sent.essayText());
        assertNull(sent.overallBand());
        assertNull(sent.grade());
        assertEquals("FREE_WRITING", jdbc.queryForObject("SELECT source FROM lesson_writing_submissions WHERE id = ?",
                String.class, sent.submissionId()));
        assertEquals(0, count("SELECT point_cost FROM lesson_writing_submissions WHERE id = ?", sent.submissionId()));
        assertEquals(1, count("SELECT count(*) FROM human_review_requests WHERE submission_id = ?", sent.submissionId()));

        var review = ArgumentCaptor.forClass(HumanReviewClient.CreateReview.class);
        verify(client).consumeCredit(sent.submissionId());
        verify(client).createReview(review.capture());
        assertEquals(EXAMINER, review.getValue().examinerId());
        assertEquals(USER, review.getValue().learnerId());
        assertEquals(sent.submissionId(), review.getValue().sourceSubmissionId());
        assertEquals("PRACTICE", review.getValue().source());
        assertEquals(TEXT, review.getValue().essay());
        noAiAndNoPoints();

        assertEquals("AWAITING_EXAMINER", getSubmission.execute(USER, sent.submissionId()).status());
        assertEquals(404, assertThrows(LearningRequestException.class,
                () -> getSubmission.execute(OTHER_USER, sent.submissionId())).getStatus());
    }

    @Test
    void sendingTheSameEssayAgainContinuesItAndStoresItOnce() {
        UUID request = UUID.randomUUID();

        var first = sendToExaminer.execute(USER, VERSION, ESSAY, request, TEXT, EXAMINER);
        var again = sendToExaminer.execute(USER, VERSION, ESSAY, request, TEXT, EXAMINER);

        assertEquals(first.submissionId(), again.submissionId());
        assertEquals(1, count("SELECT count(*) FROM lesson_writing_submissions"));
        verify(client, times(1)).createReview(any());
    }

    private static HumanReviewClient.Review review(String status) {
        return new HumanReviewClient.Review(UUID.randomUUID(), status, null, null, null, null, null, EXAMINER);
    }

    @Test
    void writingTheSamePromptAgainWhileTheFirstEssayWaitsForItsExaminerIsRefusedAndCostsNothing() {
        var first = sendToExaminer.execute(USER, VERSION, ESSAY, UUID.randomUUID(), TEXT, EXAMINER);
        when(client.fetchReview(any(), any())).thenReturn(review("PENDING"));

        // The learner left, came back to the same prompt and wrote it again: a new request id for the same prompt.
        var refused = assertThrows(LearningRequestException.class,
                () -> sendToExaminer.execute(USER, VERSION, ESSAY, UUID.randomUUID(), TEXT + "More.", OTHER_EXAMINER));

        assertEquals(409, refused.getStatus());
        assertEquals("EXAMINER_REVIEW_PENDING", refused.getCode());
        assertEquals(first.submissionId(), refused.getSubmissionId());
        assertEquals(1, count("SELECT count(*) FROM lesson_writing_submissions"));
        verify(client, times(1)).consumeCredit(any());
        verify(client, times(1)).createReview(any());
        noAiAndNoPoints();

        // The learner is told before writing, and only for their own essay.
        assertEquals(java.util.Optional.of(first.submissionId()), sendToExaminer.pending(USER, ESSAY));
        assertEquals(java.util.Optional.empty(), sendToExaminer.pending(OTHER_USER, ESSAY));
        sendToExaminer.execute(OTHER_USER, VERSION, ESSAY, UUID.randomUUID(), TEXT, EXAMINER);
        assertEquals(2, count("SELECT count(*) FROM lesson_writing_submissions"));
    }

    @Test
    void onceTheExaminerHasGradedTheLearnerMayWriteAndSendThePromptAgain() {
        var first = sendToExaminer.execute(USER, VERSION, ESSAY, UUID.randomUUID(), TEXT, EXAMINER);
        when(client.fetchReview(any(), any())).thenReturn(review("DONE"));

        assertEquals(java.util.Optional.empty(), sendToExaminer.pending(USER, ESSAY));
        var again = sendToExaminer.execute(USER, VERSION, ESSAY, UUID.randomUUID(), TEXT + "Better.", OTHER_EXAMINER);

        assertEquals(2, count("SELECT count(*) FROM lesson_writing_submissions"));
        assertEquals(false, first.submissionId().equals(again.submissionId()));
        verify(client, times(2)).createReview(any());
    }

    @Test
    void aCancelledRequestNoLongerBlocksTheLearnerFromSendingThePromptAgain() {
        sendToExaminer.execute(USER, VERSION, ESSAY, UUID.randomUUID(), TEXT, EXAMINER);
        when(client.fetchReview(any(), any())).thenReturn(review("CANCELLED"));

        assertEquals(java.util.Optional.empty(), sendToExaminer.pending(USER, ESSAY));
        sendToExaminer.execute(USER, VERSION, ESSAY, UUID.randomUUID(), TEXT + "Again.", OTHER_EXAMINER);

        assertEquals(2, count("SELECT count(*) FROM lesson_writing_submissions"));
        verify(client, times(2)).createReview(any());
    }

    @Test
    void anEssayThatWasNeverSentToAnExaminerDoesNotBlockWritingAgain() {
        when(client.consumeCredit(any())).thenThrow(new LearningRequestException(402, "HUMAN_GRADING_CREDIT_REQUIRED", "none"));
        assertThrows(LearningRequestException.class,
                () -> sendToExaminer.execute(USER, VERSION, ESSAY, UUID.randomUUID(), TEXT, EXAMINER));
        org.mockito.Mockito.doReturn(new HumanReviewClient.CreditCharge(3, true)).when(client).consumeCredit(any());

        assertEquals(java.util.Optional.empty(), sendToExaminer.pending(USER, ESSAY));
        var sent = sendToExaminer.execute(USER, VERSION, ESSAY, UUID.randomUUID(), TEXT, EXAMINER);

        assertEquals("AWAITING_EXAMINER", sent.status());
        verify(client, never()).fetchReview(any(), any());
    }

    @Test
    void withoutAPremiumCreditNothingIsSentAndTheLearnerCanSendItAgainOnceTheyHaveOne() {
        UUID request = UUID.randomUUID();
        when(client.consumeCredit(any())).thenThrow(new LearningRequestException(402, "HUMAN_GRADING_CREDIT_REQUIRED",
                "No human-grading credit is left"));

        var refused = assertThrows(LearningRequestException.class,
                () -> sendToExaminer.execute(USER, VERSION, ESSAY, request, TEXT, EXAMINER));

        assertEquals(402, refused.getStatus());
        assertEquals("HUMAN_GRADING_CREDIT_REQUIRED", refused.getCode());
        verify(client, never()).createReview(any());
        assertEquals(0, count("SELECT count(*) FROM human_review_requests"));
        noAiAndNoPoints();

        org.mockito.Mockito.doReturn(new HumanReviewClient.CreditCharge(3, true)).when(client).consumeCredit(any());
        var sent = sendToExaminer.execute(USER, VERSION, ESSAY, request, TEXT, EXAMINER);

        assertEquals("AWAITING_EXAMINER", sent.status());
        assertEquals(1, count("SELECT count(*) FROM lesson_writing_submissions"));
        assertEquals(1, count("SELECT count(*) FROM human_review_requests"));
    }

    @Test
    void anExaminerWhoCannotTakeTheEssayGivesTheCreditBackAndTheLearnerChoosesAnother() {
        UUID request = UUID.randomUUID();
        when(client.createReview(any())).thenThrow(new LearningRequestException(409, "EXAMINER_UNAVAILABLE", "full"));

        var refused = assertThrows(LearningRequestException.class,
                () -> sendToExaminer.execute(USER, VERSION, ESSAY, request, TEXT, EXAMINER));

        assertEquals("EXAMINER_UNAVAILABLE", refused.getCode());
        UUID essayId = jdbc.queryForObject("SELECT id FROM lesson_writing_submissions", UUID.class);
        verify(client).refundCredit(essayId);
        assertEquals(0, count("SELECT count(*) FROM human_review_requests"));

        reset(client);
        when(client.consumeCredit(any())).thenReturn(new HumanReviewClient.CreditCharge(2, true));
        when(client.createReview(any())).thenAnswer(invocation -> UUID.randomUUID());
        var sent = sendToExaminer.execute(USER, VERSION, ESSAY, request, TEXT, OTHER_EXAMINER);

        assertEquals(essayId, sent.submissionId());
        assertEquals(1, count("SELECT count(*) FROM lesson_writing_submissions"));
        var review = ArgumentCaptor.forClass(HumanReviewClient.CreateReview.class);
        verify(client).createReview(review.capture());
        assertEquals(OTHER_EXAMINER, review.getValue().examinerId());
    }

    @Test
    void theEssayAndThePromptAreCheckedBeforeAnythingIsStoredOrSent() {
        assertEquals("ESSAY_TOO_SHORT", assertThrows(LearningRequestException.class,
                () -> sendToExaminer.execute(USER, VERSION, ESSAY, UUID.randomUUID(), "Too short.", EXAMINER)).getCode());
        assertEquals("ESSAY_EMPTY", assertThrows(LearningRequestException.class,
                () -> sendToExaminer.execute(USER, VERSION, ESSAY, UUID.randomUUID(), " ", EXAMINER)).getCode());
        assertEquals(404, assertThrows(LearningRequestException.class,
                () -> sendToExaminer.execute(USER, VERSION, OBJECTIVE, UUID.randomUUID(), TEXT, EXAMINER)).getStatus());
        assertEquals(404, assertThrows(LearningRequestException.class,
                () -> sendToExaminer.execute(USER, VERSION, UUID.randomUUID(), UUID.randomUUID(), TEXT, EXAMINER)).getStatus());
        assertEquals("NOT_A_PRACTICE_TEST", assertThrows(LearningRequestException.class,
                () -> sendToExaminer.execute(USER, LESSON_VERSION, ESSAY, UUID.randomUUID(), TEXT, EXAMINER)).getCode());

        assertEquals(0, count("SELECT count(*) FROM lesson_writing_submissions"));
        verifyNoInteractions(client);
        noAiAndNoPoints();
    }

    @Test
    void aRequestIdAnotherLearnerUsedIsRefusedAndTheirEssayStaysTheirs() {
        UUID request = UUID.randomUUID();
        var mine = sendToExaminer.execute(USER, VERSION, ESSAY, request, TEXT, EXAMINER);

        assertEquals(409, assertThrows(LearningRequestException.class,
                () -> sendToExaminer.execute(OTHER_USER, VERSION, ESSAY, request, TEXT, EXAMINER)).getStatus());

        assertEquals(1, count("SELECT count(*) FROM lesson_writing_submissions"));
        assertEquals(USER, jdbc.queryForObject("SELECT user_id FROM lesson_writing_submissions WHERE id = ?", UUID.class,
                mine.submissionId()));
        verify(client, times(1)).createReview(any());
    }
}
