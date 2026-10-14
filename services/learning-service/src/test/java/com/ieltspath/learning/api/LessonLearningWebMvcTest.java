package com.ieltspath.learning.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.controller.LessonLearningController;
import com.ieltspath.learning.api.controller.PracticeController;
import com.ieltspath.learning.api.controller.ReviewController;
import com.ieltspath.learning.api.dto.response.ReviewResponse;
import com.ieltspath.learning.api.dto.response.ReviewSubmissionResponse;
import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.*;
import com.ieltspath.learning.application.usecase.AssignTopicTestUseCase;
import com.ieltspath.learning.application.usecase.CompleteLessonUseCase;
import com.ieltspath.learning.application.usecase.GetLessonPracticeSetsUseCase;
import com.ieltspath.learning.application.usecase.GetLessonUseCase;
import com.ieltspath.learning.application.usecase.GetMasteryUseCase;
import com.ieltspath.learning.application.usecase.GetPracticeAttemptUseCase;
import com.ieltspath.learning.application.usecase.GetReviewUseCase;
import com.ieltspath.learning.application.usecase.GetTopicLessonsUseCase;
import com.ieltspath.learning.application.usecase.ListReviewsUseCase;
import com.ieltspath.learning.application.usecase.RefreshLearningTopicsUseCase;
import com.ieltspath.learning.application.usecase.StartPracticeAttemptUseCase;
import com.ieltspath.learning.application.usecase.SubmitPracticeAttemptUseCase;
import com.ieltspath.learning.application.usecase.SubmitPracticeEssayUseCase;
import com.ieltspath.learning.application.usecase.SubmitReviewUseCase;
import com.ieltspath.learning.application.usecase.SubmitTheoryCheckUseCase;
import com.ieltspath.learning.domain.exception.LearningGateException;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PendingReview;
import com.ieltspath.learning.domain.vo.ReviewListEntry;
import com.ieltspath.learning.domain.vo.ReviewStage;
import com.ieltspath.learning.domain.vo.ReviewStatus;
import com.ieltspath.learning.domain.vo.TopicStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {LessonLearningController.class, ReviewController.class, PracticeController.class}, properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "spring.jackson.default-property-inclusion=non_null"
})
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
class LessonLearningWebMvcTest {
    private static final byte[] INTERNAL_KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    private static final UUID USER = UUID.randomUUID();
    private static final UUID TOPIC = UUID.randomUUID();
    private static final UUID LESSON = UUID.randomUUID();
    private static final UUID QUESTION = UUID.randomUUID();
    private static final UUID REVIEW = UUID.randomUUID();
    private static final UUID SOURCE_PACKAGE = UUID.randomUUID();
    private static final UUID KP = UUID.randomUUID();

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean GetTopicLessonsUseCase getTopicLessonsUseCase;
    @MockitoBean GetLessonUseCase getLessonUseCase;
    @MockitoBean CompleteLessonUseCase completeLessonUseCase;
    @MockitoBean RefreshLearningTopicsUseCase topics;
    @MockitoBean GetMasteryUseCase mastery;
    @MockitoBean ListReviewsUseCase listReviews;
    @MockitoBean SubmitTheoryCheckUseCase submitTheoryCheck;
    @MockitoBean GetReviewUseCase getReviewUseCase;
    @MockitoBean SubmitReviewUseCase submitReviewUseCase;
    @MockitoBean AssignTopicTestUseCase assignTopicTestUseCase;
    @MockitoBean GetLessonPracticeSetsUseCase practiceCatalog;
    @MockitoBean StartPracticeAttemptUseCase startPractice;
    @MockitoBean GetPracticeAttemptUseCase getPractice;
    @MockitoBean SubmitPracticeAttemptUseCase submitPractice;
    @MockitoBean SubmitPracticeEssayUseCase submitPracticeEssay;

    @BeforeEach
    void verifiedIdentity() {
        when(currentUser.requireUserId()).thenReturn(USER);
    }

    @Test
    void practiceStartAndReadHideAnswersBeforeSubmission() throws Exception {
        UUID attemptId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        var view = new PracticeAttemptView(attemptId, packageId, UUID.randomUUID(), "Passage", null,
                List.of(new LessonResult.Question(QUESTION, 1, "Question", null, "Hint")));
        when(startPractice.execute(USER, LESSON, packageId)).thenReturn(view);
        when(getPractice.execute(USER, attemptId)).thenReturn(new GetPracticeAttemptUseCase.Result(view, null));
        var started = body(mvc.perform(authenticated(post("/api/learning/lessons/{id}/practice-attempts", LESSON)
                .contentType(MediaType.APPLICATION_JSON).content("{\"packageId\":\"" + packageId + "\"}")))
                .andExpect(status().isCreated()).andReturn());
        var fetched = body(mvc.perform(authenticated(get("/api/learning/practice-attempts/{id}", attemptId)))
                .andExpect(status().isOk()).andReturn());
        assertEquals("Hint", started.path("questions").get(0).path("hint").asText());
        assertFalse(started.toString().contains("answerSpec"));
        assertFalse(started.toString().contains("explanation"));
        assertFalse(fetched.toString().contains("answerSpec"));
        assertFalse(fetched.toString().contains("explanation"));
    }

    @Test
    void listsOwnedReviewsWithSkillAndRejectsExcessiveLimit() throws Exception {
        when(listReviews.execute(USER, ReviewStatus.PENDING, LearningSkill.READING, 20))
                .thenReturn(List.of(new ReviewListEntry(REVIEW, KP, LESSON, LearningSkill.READING,
                        ReviewStage.THEORY, Instant.parse("2026-10-01T00:00:00Z"), SOURCE_PACKAGE)));
        mvc.perform(authenticated(get("/api/learning/reviews").param("skill", "READING")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skill").value("READING"))
                .andExpect(jsonPath("$[0].stage").value("THEORY"))
                .andExpect(jsonPath("$[0].sourcePackageId").value(SOURCE_PACKAGE.toString()));
        mvc.perform(authenticated(get("/api/learning/reviews").param("limit", "101")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lessonShowsItsPlaceAndStatusWithoutBlocks() throws Exception {
        when(getLessonUseCase.execute(USER, LESSON)).thenReturn(new LessonResult(LESSON, TOPIC, "L1", "First lesson",
                null, 1, "AVAILABLE", LearningSkill.READING));

        JsonNode response = body(mvc.perform(authenticated(get("/api/learning/lessons/{id}", LESSON)))
                .andExpect(status().isOk()).andReturn());

        assertEquals(Set.of("lessonId", "topicId", "code", "title", "sortOrder", "status", "skill", "skills"),
                keys(response));
        assertEquals("AVAILABLE", response.path("status").asText());
        assertEquals("READING", response.path("skills").get(0).asText());
    }

    @Test
    void reviewSetsCarryHintsAndEveryAnsweredSetShowsItsSolutions() {
        var question = new LessonResult.Question(QUESTION, 1, "Choose one", null, "Look at the first sentence.");
        var review = new ReviewResult(REVIEW, "PENDING", LESSON, List.of(), new ReviewResult.ReviewSet(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Passage", null, List.of(question)),
                KP, LearningSkill.READING, "PRACTICE", null, "KNOWLEDGE_POINT", 0, 2, List.of());
        JsonNode reviewQuestion = json.valueToTree(ReviewResponse.from(review)).path("set").path("questions").get(0);
        assertEquals(Set.of("questionVersionId", "sortOrder", "stem", "options", "hint"), keys(reviewQuestion));
        assertEquals("Look at the first sentence.", reviewQuestion.path("hint").asText());
        for (boolean passed : List.of(false, true)) {
            var result = new ReviewSubmissionResult(passed ? "DONE" : "PENDING", passed,
                    List.of(new SubmissionResult.AnswerResult(QUESTION, false, "A", "Explanation", "Hidden hint")),
                    null, passed ? "PRACTICE" : "THEORY", 1);
            JsonNode answer = json.valueToTree(ReviewSubmissionResponse.from(result)).path("results").get(0);
            assertTrue(answer.path("hint").isNull());
            assertFalse(answer.has("answerSpec"));
            assertEquals("A", answer.path("correctAnswer").asText());
            assertEquals("Explanation", answer.path("explanation").asText());
        }
    }

    enum LessonAction { READ, COMPLETE }

    @ParameterizedTest
    @EnumSource(LessonAction.class)
    void reviewGateUsesExactErrorShapeForEveryLessonAction(LessonAction action) throws Exception {
        var gate = new LearningGateException("REVIEW_REQUIRED", List.of(new PendingReview(REVIEW, LESSON, KP)));
        when(getLessonUseCase.execute(USER, LESSON)).thenThrow(gate);
        when(completeLessonUseCase.execute(USER, LESSON)).thenThrow(gate);

        JsonNode response = body(mvc.perform(authenticated(lessonAction(action)))
                .andExpect(status().isForbidden()).andReturn());
        assertEquals(Set.of("detail", "code", "reviews"), keys(response));
        assertEquals("REVIEW_REQUIRED", response.path("code").asText());
        assertFalse(response.path("detail").asText().isBlank());
        JsonNode review = response.path("reviews").get(0);
        assertEquals(Set.of("reviewId", "lessonId", "knowledgePointId"), keys(review));
        assertEquals(REVIEW.toString(), review.path("reviewId").asText());
        assertEquals(LESSON.toString(), review.path("lessonId").asText());
        assertEquals(KP.toString(), review.path("knowledgePointId").asText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TOPIC_LOCKED", "LESSON_LOCKED"})
    void otherGatesOmitReviews(String code) throws Exception {
        when(getLessonUseCase.execute(USER, LESSON)).thenThrow(new LearningGateException(code, List.of()));
        JsonNode response = body(mvc.perform(authenticated(get("/api/learning/lessons/{id}", LESSON)))
                .andExpect(status().isForbidden()).andReturn());
        assertEquals(Set.of("detail", "code"), keys(response));
        assertEquals(code, response.path("code").asText());
    }

    @Test
    void listMasteryAndCompletionRoutesUseVerifiedIdentity() throws Exception {
        when(topics.execute(USER)).thenReturn(List.of(
                new TopicResult(TOPIC, "FIRST", "First topic", 1, TopicStatus.IN_PROGRESS, 2, null, null, true, null,
                        false, null, 4),
                new TopicResult(UUID.randomUUID(), "PAID", "Paid topic", 2, TopicStatus.LOCKED, 0,
                        "PREMIUM_CONTENT")));
        when(getTopicLessonsUseCase.execute(USER, TOPIC)).thenReturn(new TopicLessonsResult(TOPIC,
                List.of(new TopicLessonsResult.LessonSummary(LESSON, "L1", "First lesson", 1, "COMPLETED", null, null,
                                15, null),
                        new TopicLessonsResult.LessonSummary(UUID.randomUUID(), "L2", "Second lesson", 2, "LOCKED",
                                null, null, null, REVIEW)),
                "AVAILABLE"));
        when(mastery.execute(USER)).thenReturn(List.of(new MasteryResult(KP, TOPIC, 0.729, 4)));
        when(completeLessonUseCase.execute(USER, LESSON)).thenReturn(LESSON);

        mvc.perform(authenticated(get("/api/learning/topics")).header("X-User-Id", UUID.randomUUID()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].completedLessonCount").value(2))
                .andExpect(jsonPath("$[0].totalLessonCount").value(4))
                .andExpect(jsonPath("$[0].accessLevel").value("FREE"))
                .andExpect(jsonPath("$[1].accessLevel").value("PREMIUM"));
        mvc.perform(authenticated(get("/api/learning/topics/{id}/lessons", TOPIC)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.testStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.lessons[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.lessons[0].estimatedMinutes").value(15))
                .andExpect(jsonPath("$.lessons[0].blockingReviewId").doesNotExist())
                .andExpect(jsonPath("$.lessons[1].status").value("LOCKED"))
                .andExpect(jsonPath("$.lessons[1].blockingReviewId").value(REVIEW.toString()));
        mvc.perform(authenticated(get("/api/learning/mastery")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].knowledgePointId").value(KP.toString()))
                .andExpect(jsonPath("$[0].mastery").value(0.729)).andExpect(jsonPath("$[0].evidenceCount").value(4));
        mvc.perform(authenticated(post("/api/learning/lessons/{id}/complete", LESSON)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lessonId").value(LESSON.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        verify(topics).execute(USER);
        verify(getTopicLessonsUseCase).execute(USER, TOPIC);
        verify(mastery).execute(USER);
        verify(completeLessonUseCase).execute(USER, LESSON);
    }

    enum ProtectedRoute { TOPICS, TOPIC_LESSONS, LESSON, COMPLETE, MASTERY }

    @ParameterizedTest
    @EnumSource(ProtectedRoute.class)
    void everyLearnerRouteRequiresAuthentication(ProtectedRoute route) throws Exception {
        MockHttpServletRequestBuilder request = switch (route) {
            case TOPICS -> get("/api/learning/topics");
            case TOPIC_LESSONS -> get("/api/learning/topics/{id}/lessons", TOPIC);
            case LESSON -> get("/api/learning/lessons/{id}", LESSON);
            case COMPLETE -> post("/api/learning/lessons/{id}/complete", LESSON);
            case MASTERY -> get("/api/learning/mastery");
        };
        mvc.perform(request).andExpect(status().isUnauthorized());
        verifyNoInteractions(topics, getTopicLessonsUseCase, getLessonUseCase, completeLessonUseCase, mastery,
                currentUser);
    }

    private MockHttpServletRequestBuilder lessonAction(LessonAction action) throws Exception {
        return switch (action) {
            case READ -> get("/api/learning/lessons/{id}", LESSON);
            case COMPLETE -> post("/api/learning/lessons/{id}/complete", LESSON);
        };
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.with(jwt().jwt(token -> token.subject(USER.toString())));
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    private Set<String> keys(JsonNode node) {
        Set<String> fields = new HashSet<>();
        node.fieldNames().forEachRemaining(fields::add);
        return fields;
    }

}
