package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.learning.application.usecase.GetStreaksUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalStreakController.class, properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
class InternalStreakControllerTest {
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean GetStreaksUseCase streaks;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void returnsTheStreakOfEachRequestedLearner() throws Exception {
        UUID first = UUID.randomUUID();
        when(streaks.execute(anyCollection())).thenReturn(Map.of(first, 5));

        mockMvc.perform(post("/internal/learning/streaks").contentType(APPLICATION_JSON)
                        .content("{\"userIds\":[\"" + first + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streaks.length()").value(1))
                .andExpect(jsonPath("$.streaks[0].userId").value(first.toString()))
                .andExpect(jsonPath("$.streaks[0].currentDays").value(5));
    }

    @Test
    void anonymousCallersAreRefused() throws Exception {
        mockMvc.perform(post("/internal/learning/streaks").contentType(APPLICATION_JSON)
                .content("{\"userIds\":[\"" + UUID.randomUUID() + "\"]}")).andExpect(status().isUnauthorized());
        verifyNoInteractions(streaks);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void refusesMoreThanOneHundredLearnersInOneBatch() throws Exception {
        String ids = IntStream.range(0, 101).mapToObj(i -> "\"" + UUID.randomUUID() + "\"")
                .collect(Collectors.joining(","));
        mockMvc.perform(post("/internal/learning/streaks").contentType(APPLICATION_JSON)
                .content("{\"userIds\":[" + ids + "]}")).andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(streaks);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void refusesARequestWithoutLearnerIds() throws Exception {
        mockMvc.perform(post("/internal/learning/streaks").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(streaks);
    }
}
