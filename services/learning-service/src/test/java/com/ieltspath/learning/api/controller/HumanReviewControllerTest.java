package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.usecase.HumanReviewUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HumanReviewController.class, properties = {"spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@Import(CommonSecurityAutoConfiguration.class)
class HumanReviewControllerTest {
    static final byte[] KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean HumanReviewUseCase reviews;
    final UUID learner = UUID.randomUUID();
    final UUID essay = UUID.randomUUID();

    @Test
    void aLearnerRequestsTheReviewOfTheirEssayAnd202ComesBackWithItsId() throws Exception {
        UUID review = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(learner);
        when(reviews.request(learner, essay)).thenReturn(review);

        mvc.perform(post("/api/learning/writing-submissions/{id}/human-review", essay)
                        .with(user("l").roles("CUSTOMER")))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.reviewSubmissionId").value(review.toString()))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    void noCreditComesBackAs402WithItsCode() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(reviews.request(learner, essay)).thenThrow(new LearningRequestException(402,
                "HUMAN_GRADING_CREDIT_REQUIRED", "none"));

        mvc.perform(post("/api/learning/writing-submissions/{id}/human-review", essay)
                        .with(user("l").roles("CUSTOMER")))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("HUMAN_GRADING_CREDIT_REQUIRED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXAMINER", "ADMIN", "CONTENT_AUTHOR", "SALES_STAFF"})
    void staffCannotRequestOrReadALearnersMentorReview(String role) throws Exception {
        mvc.perform(post("/api/learning/writing-submissions/{id}/human-review", essay).with(user("s").roles(role)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/learning/writing-submissions/{id}/human-review", essay).with(user("s").roles(role)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reviews);
    }

    @Test
    void anonymousCallersAreRejected() throws Exception {
        mvc.perform(post("/api/learning/writing-submissions/{id}/human-review", essay))
                .andExpect(status().isUnauthorized());
    }
}
