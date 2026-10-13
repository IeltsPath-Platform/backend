package com.ieltspath.learning.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.controller.WritingController;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.WritingSubmissionResult;
import com.ieltspath.learning.application.usecase.GetWritingSubmissionUseCase;
import com.ieltspath.learning.domain.vo.WritingGrade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WritingController.class, properties = {
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
    private static final UUID SUBMISSION = UUID.randomUUID();

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean GetWritingSubmissionUseCase getWritingSubmissionUseCase;

    @BeforeEach
    void verifiedIdentity() {
        when(currentUser.requireUserId()).thenReturn(USER);
    }

    @Test
    void gradedEssayHasTheContractKeysAndNothingServerSide() throws Exception {
        var grade = new WritingGrade(List.of(new WritingGrade.Criterion("TR", new BigDecimal("6.5"), List.of("s"),
                List.of("i"))), List.of(new WritingGrade.Correction("peoples", "people", "GRAMMAR")), "ok",
                new BigDecimal("6.5"));
        when(getWritingSubmissionUseCase.execute(USER, SUBMISSION)).thenReturn(new WritingSubmissionResult(
                SUBMISSION, "GRADED", "TASK_2", 260, new BigDecimal("6.5"), true, grade, 3, "Model", null, null));

        JsonNode body = json.readTree(mvc.perform(authenticated(get("/api/learning/writing-submissions/{id}",
                        SUBMISSION))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        assertEquals(Set.of("submissionId", "status", "task", "wordCount", "overallBand", "passed", "criteria",
                "corrections", "summary", "pointsCharged", "sampleAnswer"), keys(body));
        assertEquals(Set.of("code", "band", "strengths", "improvements"), keys(body.get("criteria").get(0)));
        assertEquals(6.5, body.get("overallBand").asDouble());
    }

    @Test
    void pendingPaymentShowsOnlyTheCode() throws Exception {
        when(getWritingSubmissionUseCase.execute(USER, SUBMISSION)).thenReturn(new WritingSubmissionResult(SUBMISSION, "PAYMENT_PENDING",
                "TASK_2", null, null, null, null, null, null, "INSUFFICIENT_POINTS", null));

        JsonNode pending = json.readTree(mvc.perform(authenticated(get("/api/learning/writing-submissions/{id}",
                SUBMISSION))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        assertEquals(Set.of("submissionId", "status", "code"), keys(pending));
    }

    @Test
    void anotherLearnersOrUnknownSubmissionIsNotFound() throws Exception {
        when(getWritingSubmissionUseCase.execute(USER, SUBMISSION))
                .thenThrow(new LearningRequestException(404, "NOT_FOUND", "Submission was not found"));

        mvc.perform(authenticated(get("/api/learning/writing-submissions/{id}", SUBMISSION)))
                .andExpect(status().isNotFound());
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
