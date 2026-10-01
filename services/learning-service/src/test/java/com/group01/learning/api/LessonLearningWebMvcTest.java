package com.group01.learning.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learning.api.controller.LessonLearningController;
import com.group01.learning.application.command.SubmitExerciseCommand;
import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.result.*;
import com.group01.learning.application.usecase.GetMasteryUseCase;
import com.group01.learning.application.usecase.LearnLessonUseCase;
import com.group01.learning.application.usecase.RefreshLearningTopicsUseCase;
import com.group01.learning.domain.exception.LearningGateException;
import com.group01.learning.domain.vo.PendingReview;
import com.group01.learning.domain.vo.TopicStatus;
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

@WebMvcTest(controllers = LessonLearningController.class, properties = {
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
    private static final UUID BLOCK = UUID.randomUUID();
    private static final UUID QUESTION = UUID.randomUUID();
    private static final UUID FILL_QUESTION = UUID.randomUUID();
    private static final UUID REQUEST = UUID.randomUUID();
    private static final UUID REVIEW = UUID.randomUUID();
    private static final UUID KP = UUID.randomUUID();

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean LearnLessonUseCase lessons;
    @MockitoBean RefreshLearningTopicsUseCase topics;
    @MockitoBean GetMasteryUseCase mastery;

    @BeforeEach
    void verifiedIdentity() {
        when(currentUser.requireUserId()).thenReturn(USER);
    }

    @Test
    void unpassedQuestionsHaveExactAllowlistAndPreserveNullFillOptions() throws Exception {
        when(lessons.get(USER, LESSON)).thenReturn(lesson(false));
        JsonNode response = body(mvc.perform(authenticated(get("/api/learning/lessons/{id}", LESSON)))
                .andExpect(status().isOk()).andReturn());

        JsonNode passage = response.path("blocks").get(0).path("asset");
        assertEquals("Passage text", passage.path("textContent").asText());
        JsonNode block = response.path("blocks").get(1);
        assertEquals(Set.of("blockId", "blockType", "blockKind", "sortOrder", "passed", "questions"), keys(block));
        assertFalse(block.path("passed").asBoolean());
        assertFalse(block.has("solutions"));
        assertQuestionAllowlist(block.path("questions").get(0));
        assertEquals(Set.of("optionKey", "content", "sortOrder"), keys(
                block.path("questions").get(0).path("options").get(0)));
        JsonNode fill = block.path("questions").get(1);
        assertQuestionAllowlist(fill);
        assertTrue(fill.has("options"));
        assertTrue(fill.path("options").isNull());
        assertFalse(response.toString().contains("answerSpec"));
        assertFalse(response.toString().contains("explanation"));
        assertFalse(response.toString().contains("knowledgePointIds"));
        assertFalse(response.toString().contains("correctAnswer"));
    }

    @Test
    void passedBlockRevealsSolutionsSeparatelyFromAllowlistedQuestions() throws Exception {
        when(lessons.get(USER, LESSON)).thenReturn(lesson(true));
        JsonNode response = body(mvc.perform(authenticated(get("/api/learning/lessons/{id}", LESSON)))
                .andExpect(status().isOk()).andReturn());
        JsonNode block = response.path("blocks").get(1);
        assertTrue(block.path("passed").asBoolean());
        assertQuestionAllowlist(block.path("questions").get(0));
        assertQuestionAllowlist(block.path("questions").get(1));
        JsonNode solution = block.path("solutions").get(0);
        assertEquals(Set.of("questionVersionId", "correctAnswer", "explanation"), keys(solution));
        assertEquals("A", solution.path("correctAnswer").asText());
        assertEquals("The main idea", solution.path("explanation").asText());
        assertFalse(block.has("answerSpec"));
        assertFalse(block.has("knowledgePointIds"));
    }

    @Test
    void failedSubmissionReturnsCorrectnessWithoutAnswerOrExplanation() throws Exception {
        when(lessons.submit(eq(USER), eq(LESSON), eq(BLOCK), any())).thenReturn(new SubmissionResult(
                false, false, List.of(new SubmissionResult.AnswerResult(QUESTION, false,
                "A", "The main idea"))));
        JsonNode response = body(mvc.perform(authenticated(submission(validBody())))
                .andExpect(status().isOk()).andReturn());
        assertEquals(Set.of("blockPassed", "lessonCompleted", "results"), keys(response));
        assertEquals(Set.of("questionVersionId", "correct"), keys(response.path("results").get(0)));
        assertFalse(response.path("results").get(0).path("correct").asBoolean());
        verify(lessons).submit(USER, LESSON, BLOCK,
                new SubmitExerciseCommand(REQUEST, List.of(new SubmitExerciseCommand.Answer(QUESTION, "A"))));
    }

    @Test
    void passedSubmissionIncludesAnswerAndExplanation() throws Exception {
        when(lessons.submit(eq(USER), eq(LESSON), eq(BLOCK), any())).thenReturn(new SubmissionResult(
                true, true, List.of(new SubmissionResult.AnswerResult(QUESTION, true,
                "A", "The main idea"))));
        JsonNode response = body(mvc.perform(authenticated(submission(validBody())))
                .andExpect(status().isOk()).andReturn());
        JsonNode answer = response.path("results").get(0);
        assertEquals(Set.of("questionVersionId", "correct", "correctAnswer", "explanation"), keys(answer));
        assertEquals("A", answer.path("correctAnswer").asText());
    }

    enum LessonAction { READ, SUBMIT, COMPLETE }

    @ParameterizedTest
    @EnumSource(LessonAction.class)
    void reviewGateUsesExactErrorShapeForEveryLessonAction(LessonAction action) throws Exception {
        var gate = new LearningGateException("REVIEW_REQUIRED", List.of(new PendingReview(REVIEW, LESSON, KP)));
        when(lessons.get(USER, LESSON)).thenThrow(gate);
        when(lessons.submit(eq(USER), eq(LESSON), eq(BLOCK), any())).thenThrow(gate);
        when(lessons.complete(USER, LESSON)).thenThrow(gate);

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
        when(lessons.get(USER, LESSON)).thenThrow(new LearningGateException(code, List.of()));
        JsonNode response = body(mvc.perform(authenticated(get("/api/learning/lessons/{id}", LESSON)))
                .andExpect(status().isForbidden()).andReturn());
        assertEquals(Set.of("detail", "code"), keys(response));
        assertEquals(code, response.path("code").asText());
    }

    static Stream<String> invalidBodies() {
        return Stream.of("{}", "{\"requestId\":\"" + REQUEST + "\"}",
                "{\"requestId\":\"" + REQUEST + "\",\"answers\":null}",
                "{\"requestId\":\"" + REQUEST + "\",\"answers\":[null]}",
                "{\"requestId\":\"" + REQUEST + "\",\"answers\":[{\"answer\":\"A\"}]}",
                "{\"requestId\":\"" + REQUEST + "\",\"answers\":[{\"questionVersionId\":\""
                        + QUESTION + "\",\"answer\":1}]}", "{malformed");
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void malformedRequestsReturn422WithoutCallingUseCase(String request) throws Exception {
        JsonNode response = body(mvc.perform(authenticated(submission(request)))
                .andExpect(status().isUnprocessableEntity()).andReturn());
        assertEquals(Set.of("detail"), keys(response));
        assertEquals("Invalid request", response.path("detail").asText());
        verifyNoInteractions(lessons);
    }

    static Stream<List<Map<String, Object>>> invalidAnswerSets() {
        Map<String, Object> answer = Map.of("questionVersionId", QUESTION, "answer", "A");
        return Stream.of(List.of(), List.of(answer, answer),
                List.of(Map.of("questionVersionId", UUID.randomUUID(), "answer", "A")));
    }

    @ParameterizedTest
    @MethodSource("invalidAnswerSets")
    void missingDuplicateAndForeignAnswersMapUseCaseFailureTo422(List<Map<String, Object>> answers)
            throws Exception {
        when(lessons.submit(eq(USER), eq(LESSON), eq(BLOCK), any())).thenThrow(new LearningRequestException(
                422, "INVALID_ANSWERS", "Submit one answer for every exercise question"));
        String request = json.writeValueAsString(Map.of("requestId", REQUEST, "answers", answers));
        JsonNode response = body(mvc.perform(authenticated(submission(request)))
                .andExpect(status().isUnprocessableEntity()).andReturn());
        assertEquals(Set.of("detail", "code"), keys(response));
        assertEquals("INVALID_ANSWERS", response.path("code").asText());
        verify(lessons).submit(eq(USER), eq(LESSON), eq(BLOCK), any());
    }

    @Test
    void requestConflictMapsTo409WithoutReviews() throws Exception {
        when(lessons.submit(eq(USER), eq(LESSON), eq(BLOCK), any())).thenThrow(new LearningRequestException(
                409, "REQUEST_CONFLICT", "requestId belongs to another submission"));
        JsonNode response = body(mvc.perform(authenticated(submission(validBody())))
                .andExpect(status().isConflict()).andReturn());
        assertEquals(Set.of("detail", "code"), keys(response));
        assertEquals("REQUEST_CONFLICT", response.path("code").asText());
    }

    @Test
    void listMasteryAndCompletionRoutesUseVerifiedIdentity() throws Exception {
        when(topics.execute(USER)).thenReturn(List.of(new TopicResult(TOPIC, "FIRST", "First topic",
                1, TopicStatus.IN_PROGRESS, 2)));
        when(lessons.list(USER, TOPIC)).thenReturn(new TopicLessonsResult(TOPIC,
                List.of(new TopicLessonsResult.LessonSummary(LESSON, "L1", "First lesson", 1, "COMPLETED")),
                "AVAILABLE"));
        when(mastery.execute(USER)).thenReturn(List.of(new MasteryResult(KP, TOPIC, 0.729, 4)));
        when(lessons.complete(USER, LESSON)).thenReturn(LESSON);

        mvc.perform(authenticated(get("/api/learning/topics")).header("X-User-Id", UUID.randomUUID()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].completedLessonCount").value(2));
        mvc.perform(authenticated(get("/api/learning/topics/{id}/lessons", TOPIC)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.testStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.lessons[0].status").value("COMPLETED"));
        mvc.perform(authenticated(get("/api/learning/mastery")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].knowledgePointId").value(KP.toString()))
                .andExpect(jsonPath("$[0].mastery").value(0.729)).andExpect(jsonPath("$[0].evidenceCount").value(4));
        mvc.perform(authenticated(post("/api/learning/lessons/{id}/complete", LESSON)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lessonId").value(LESSON.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        verify(topics).execute(USER);
        verify(lessons).list(USER, TOPIC);
        verify(mastery).execute(USER);
        verify(lessons).complete(USER, LESSON);
    }

    enum ProtectedRoute { TOPICS, TOPIC_LESSONS, LESSON, SUBMISSION, COMPLETE, MASTERY }

    @ParameterizedTest
    @EnumSource(ProtectedRoute.class)
    void everyLearnerRouteRequiresAuthentication(ProtectedRoute route) throws Exception {
        MockHttpServletRequestBuilder request = switch (route) {
            case TOPICS -> get("/api/learning/topics");
            case TOPIC_LESSONS -> get("/api/learning/topics/{id}/lessons", TOPIC);
            case LESSON -> get("/api/learning/lessons/{id}", LESSON);
            case SUBMISSION -> submission(validBody());
            case COMPLETE -> post("/api/learning/lessons/{id}/complete", LESSON);
            case MASTERY -> get("/api/learning/mastery");
        };
        mvc.perform(request).andExpect(status().isUnauthorized());
        verifyNoInteractions(topics, lessons, mastery, currentUser);
    }

    private LessonResult lesson(boolean passed) {
        var choice = new LessonResult.Question(QUESTION, 1, "Choose the main idea",
                List.of(new LessonResult.Option("A", "Main idea", 1)));
        var fill = new LessonResult.Question(FILL_QUESTION, 2, "Complete one word", null);
        var asset = new LessonResult.Asset(UUID.randomUUID(), "PASSAGE", "Passage text", null, null, null);
        var passage = new LessonResult.Block(UUID.randomUUID(), "ASSET", null, 1, null, asset, null,
                null, null, null, null);
        var exercise = new LessonResult.Block(BLOCK, "EXERCISE", "EXERCISE", 2, null, null, null,
                passed, List.of(choice, fill), passed ? List.of(
                new LessonResult.Solution(QUESTION, "A", "The main idea"),
                new LessonResult.Solution(FILL_QUESTION, "critics", "A word from the passage")) : null, null);
        return new LessonResult(LESSON, TOPIC, "L1", "First lesson", null, 1, "AVAILABLE",
                List.of(passage, exercise));
    }

    private MockHttpServletRequestBuilder lessonAction(LessonAction action) throws Exception {
        return switch (action) {
            case READ -> get("/api/learning/lessons/{id}", LESSON);
            case SUBMIT -> submission(validBody());
            case COMPLETE -> post("/api/learning/lessons/{id}/complete", LESSON);
        };
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.with(jwt().jwt(token -> token.subject(USER.toString())));
    }

    private MockHttpServletRequestBuilder submission(String request) {
        return post("/api/learning/lessons/{id}/exercises/{blockId}/submissions", LESSON, BLOCK)
                .contentType(MediaType.APPLICATION_JSON).content(request);
    }

    private String validBody() throws Exception {
        return json.writeValueAsString(Map.of("requestId", REQUEST, "answers",
                List.of(Map.of("questionVersionId", QUESTION, "answer", "A"))));
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    private Set<String> keys(JsonNode node) {
        Set<String> fields = new HashSet<>();
        node.fieldNames().forEachRemaining(fields::add);
        return fields;
    }

    private void assertQuestionAllowlist(JsonNode question) {
        assertEquals(Set.of("questionVersionId", "sortOrder", "stem", "options"), keys(question));
    }
}
