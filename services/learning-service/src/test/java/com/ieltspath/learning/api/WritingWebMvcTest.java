package com.ieltspath.learning.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.controller.LessonLearningController;
import com.ieltspath.learning.api.controller.WritingController;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.LessonResult;
import com.ieltspath.learning.application.result.WritingSubmissionResult;
import com.ieltspath.learning.application.usecase.GetMasteryUseCase;
import com.ieltspath.learning.application.usecase.GetTopicLessonsUseCase;
import com.ieltspath.learning.application.usecase.GetLessonUseCase;
import com.ieltspath.learning.application.usecase.SubmitLessonExerciseUseCase;
import com.ieltspath.learning.application.usecase.CompleteLessonUseCase;
import com.ieltspath.learning.application.usecase.GetWritingSubmissionUseCase;
import com.ieltspath.learning.application.usecase.SubmitLessonEssayUseCase;
import com.ieltspath.learning.application.usecase.RefreshLearningTopicsUseCase;
import com.ieltspath.learning.domain.vo.WritingGrade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {WritingController.class, LessonLearningController.class}, properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "spring.jackson.default-property-inclusion=non_null"
})
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
class WritingWebMvcTest {
    private static final byte[] INTERNAL_KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    private static final UUID USER = UUID.randomUUID();
    private static final UUID LESSON = UUID.randomUUID();
    private static final UUID BLOCK = UUID.randomUUID();
    private static final UUID SUBMISSION = UUID.randomUUID();

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean GetWritingSubmissionUseCase getWritingSubmissionUseCase;
    @MockitoBean SubmitLessonEssayUseCase submitLessonEssayUseCase;
    @MockitoBean GetTopicLessonsUseCase getTopicLessonsUseCase;
    @MockitoBean GetLessonUseCase getLessonUseCase;
    @MockitoBean SubmitLessonExerciseUseCase submitLessonExerciseUseCase;
    @MockitoBean CompleteLessonUseCase completeLessonUseCase;
    @MockitoBean RefreshLearningTopicsUseCase topics;
    @MockitoBean GetMasteryUseCase mastery;

    @BeforeEach
    void verifiedIdentity() {
        when(currentUser.requireUserId()).thenReturn(USER);
    }

    @Test
    void gradedEssayHasTheContractKeysAndNothingServerSide() throws Exception {
        var grade = new WritingGrade(List.of(new WritingGrade.Criterion("TR", new BigDecimal("6.5"), List.of("s"),
                List.of("i"))), List.of(new WritingGrade.Correction("peoples", "people", "GRAMMAR")), "ok",
                new BigDecimal("6.5"));
        when(submitLessonEssayUseCase.execute(eq(USER), eq(LESSON), eq(BLOCK), any(), eq("essay"))).thenReturn(new WritingSubmissionResult(
                SUBMISSION, "GRADED", "TASK_2", 260, new BigDecimal("6.5"), true, grade, 3, "Model", null, null));

        JsonNode body = json.readTree(mvc.perform(authenticated(post(
                        "/api/learning/lessons/{lessonId}/essays/{blockId}/submissions", LESSON, BLOCK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"%s\",\"essayText\":\"essay\"}".formatted(UUID.randomUUID()))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        assertEquals(Set.of("submissionId", "status", "task", "wordCount", "overallBand", "passed", "criteria",
                "corrections", "summary", "pointsCharged", "sampleAnswer"), keys(body));
        assertEquals(Set.of("code", "band", "strengths", "improvements"), keys(body.get("criteria").get(0)));
        assertEquals(6.5, body.get("overallBand").asDouble());
    }

    @Test
    void pendingPaymentShowsOnlyTheCodeAndErrorsCarryTheSubmission() throws Exception {
        when(getWritingSubmissionUseCase.execute(USER, SUBMISSION)).thenReturn(new WritingSubmissionResult(SUBMISSION, "PAYMENT_PENDING",
                "TASK_2", null, null, null, null, null, null, "INSUFFICIENT_POINTS", null));
        JsonNode pending = json.readTree(mvc.perform(authenticated(get("/api/learning/writing-submissions/{id}",
                SUBMISSION))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals(Set.of("submissionId", "status", "code"), keys(pending));

        when(submitLessonEssayUseCase.execute(any(), any(), any(), any(), any())).thenThrow(new LearningRequestException(402,
                "INSUFFICIENT_POINTS", "Not enough points", SUBMISSION));
        JsonNode error = json.readTree(mvc.perform(authenticated(post(
                        "/api/learning/lessons/{lessonId}/essays/{blockId}/submissions", LESSON, BLOCK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"%s\",\"essayText\":\"x\"}".formatted(UUID.randomUUID()))))
                .andExpect(status().isPaymentRequired()).andReturn().getResponse().getContentAsString());
        assertEquals("INSUFFICIENT_POINTS", error.get("code").asText());
        assertEquals(SUBMISSION.toString(), error.get("submissionId").asText());
    }

    @Test
    void essayBlockAlwaysShowsLatestSubmissionAndNeverChartFacts() throws Exception {
        var question = new LessonResult.EssayQuestion(UUID.randomUUID(), "Describe the chart.", "TASK_1", 150,
                new BigDecimal("6.0"), List.of(new LessonResult.Image("https://cdn/chart.svg", "Chart")));
        var none = new LessonResult.Block(BLOCK, "EXERCISE", "ESSAY", 1, null, null, null, null, null, null, question);
        var latest = new LessonResult.Block(UUID.randomUUID(), "EXERCISE", "ESSAY", 2, null, null, null, null, null,
                null, question, new LessonResult.LatestSubmission(SUBMISSION, "GRADED", new BigDecimal("7.0"), true),
                "Model");
        when(getLessonUseCase.execute(USER, LESSON)).thenReturn(new LessonResult(LESSON, UUID.randomUUID(), "L3", "Chart", null, 1,
                "AVAILABLE", List.of(none, latest)));

        String raw = mvc.perform(authenticated(get("/api/learning/lessons/{id}", LESSON)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode blocks = json.readTree(raw).get("blocks");
        assertTrue(blocks.get(0).has("latestSubmission"));
        assertTrue(blocks.get(0).get("latestSubmission").isNull());
        assertFalse(blocks.get(0).has("sampleAnswer"));
        assertEquals("GRADED", blocks.get(1).get("latestSubmission").get("status").asText());
        assertEquals("Model", blocks.get(1).get("sampleAnswer").asText());
        assertEquals(Set.of("questionVersionId", "stem", "task", "minWords", "passBand", "images"),
                keys(blocks.get(0).get("question")));
        assertFalse(raw.contains("chartFacts"));
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.with(jwt().jwt(token -> token.subject(USER.toString())));
    }

    private static Set<String> keys(JsonNode node) {
        Set<String> fields = new HashSet<>();
        node.fieldNames().forEachRemaining(fields::add);
        return fields;
    }
}
