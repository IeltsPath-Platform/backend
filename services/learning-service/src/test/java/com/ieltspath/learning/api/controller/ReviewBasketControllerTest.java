package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.ReviewBasketResult;
import com.ieltspath.learning.application.usecase.GetReviewBasketUseCase;
import com.ieltspath.learning.application.usecase.StartBasketReviewUseCase;
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
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReviewBasketController.class, properties = {"spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@Import(CommonSecurityAutoConfiguration.class)
class ReviewBasketControllerTest {
    static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean GetReviewBasketUseCase basket;
    @MockitoBean StartBasketReviewUseCase start;
    final UUID learner = UUID.randomUUID();
    final UUID point = UUID.randomUUID();

    @Test
    void theBasketUsesTheVerifiedIdentityAndShowsThreeGroups() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(basket.execute(learner)).thenReturn(new ReviewBasketResult(List.of(),
                List.of(new ReviewBasketResult.OptionalItem(point, "KP", "Point", "READING", "CONSOLIDATE", 0.65,
                        Instant.parse("2026-10-20T00:00:00Z"), "CONSOLIDATE", true, null)), List.of()));

        mvc.perform(get("/api/learning/review-basket").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.required").isEmpty())
                .andExpect(jsonPath("$.consolidate[0].knowledgePointId").value(point.toString()))
                .andExpect(jsonPath("$.consolidate[0].reason").value("CONSOLIDATE"))
                .andExpect(jsonPath("$.consolidate[0].hasUnrevealedSet").value(true))
                .andExpect(jsonPath("$.due").isEmpty());
        verify(basket).execute(learner);
    }

    @Test
    void startMapsAMissingSetTo409() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(start.execute(learner, point)).thenThrow(
                new LearningRequestException(409, "NO_UNREVEALED_SET", "No practice set is left"));

        mvc.perform(post("/api/learning/review-basket/" + point + "/start").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_UNREVEALED_SET"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "EXAMINER", "CONTENT_AUTHOR"})
    void onlyACustomerUsesTheBasket(String role) throws Exception {
        mvc.perform(get("/api/learning/review-basket").with(user("staff").roles(role))).andExpect(status().isForbidden());
        mvc.perform(post("/api/learning/review-basket/" + point + "/start").with(user("staff").roles(role)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(basket, start);
    }

    @Test
    void unauthenticatedCallersAreRejected() throws Exception {
        mvc.perform(get("/api/learning/review-basket")).andExpect(status().isUnauthorized());
    }
}
