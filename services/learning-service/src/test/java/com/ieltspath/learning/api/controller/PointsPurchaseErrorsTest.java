package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.exception.InsufficientPointsException;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.usecase.HeartsUseCase;
import com.ieltspath.learning.application.usecase.StreakUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** What a learner sees when a heart refill or a streak freeze cannot be paid for. */
@WebMvcTest(controllers = {HeartsController.class, StreakController.class}, properties = {
        "spring.cloud.config.enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@Import(CommonSecurityAutoConfiguration.class)
class PointsPurchaseErrorsTest {
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
    @MockitoBean HeartsUseCase hearts;
    @MockitoBean StreakUseCase streak;
    final UUID learner = UUID.randomUUID();
    final String body = "{\"requestId\":\"" + UUID.randomUUID() + "\"}";

    @Test
    void aRefillWithTooFewPointsIsPaymentRequired() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(hearts.refill(any(), any())).thenThrow(new InsufficientPointsException());

        mvc.perform(post("/api/learning/me/hearts/refill").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_POINTS"));
    }

    @Test
    void aFreezeWithTooFewPointsIsPaymentRequired() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(streak.buyFreeze(any(), any())).thenThrow(new InsufficientPointsException());

        mvc.perform(post("/api/learning/me/streak/freeze").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_POINTS"));
    }

    @Test
    void whenAccessIsDownThePurchaseIsServiceUnavailableAndSaysNothingWasCharged() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(hearts.refill(any(), any())).thenThrow(new AccessUnavailableException(503));

        mvc.perform(post("/api/learning/me/hearts/refill").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ACCESS_UNAVAILABLE"));
    }

    @Test
    void theStreakRulesErrorsKeepTheirCodes() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(streak.buyFreeze(any(), any())).thenThrow(
                new LearningRequestException(409, "FREEZE_ALREADY_OWNED", "A freeze is already owned"));

        mvc.perform(post("/api/learning/me/streak/freeze").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FREEZE_ALREADY_OWNED"));
    }

    @Test
    void aFreezeRequestWithoutARequestIdIsRejected() throws Exception {
        mvc.perform(post("/api/learning/me/streak/freeze").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isUnprocessableEntity());
    }
}
