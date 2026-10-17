package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.content.application.usecase.GetQuestionDifficultiesUseCase;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalQuestionDifficultyController.class, properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Import(CommonSecurityAutoConfiguration.class)
class InternalQuestionDifficultyControllerTest {
    private static final byte[] INTERNAL_KEY = new byte[32];

    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GetQuestionDifficultiesUseCase difficulties;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/internal/learning-content/questions/difficulties")
                        .contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(difficulties);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void returnsDifficultyTagsInOneBatch() throws Exception {
        UUID easyId = UUID.randomUUID();
        UUID hardId = UUID.randomUUID();
        when(difficulties.execute(List.of(easyId, hardId))).thenReturn(Map.of(
                easyId, QuestionDifficulty.EASY, hardId, QuestionDifficulty.HARD));

        mockMvc.perform(post("/internal/learning-content/questions/difficulties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionVersionIds\":[\"" + easyId + "\",\"" + hardId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficulties['" + easyId + "']").value("EASY"))
                .andExpect(jsonPath("$.difficulties['" + hardId + "']").value("HARD"));
        verify(difficulties).execute(List.of(easyId, hardId));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void rejectsEmptyOversizedAndNullBatchesBeforeQuerying() throws Exception {
        String fiveHundredOne = String.join(",", java.util.Collections.nCopies(501,
                "\"" + UUID.randomUUID() + "\""));
        for (String payload : List.of("{\"questionVersionIds\":[]}",
                "{\"questionVersionIds\":[" + fiveHundredOne + "]}",
                "{\"questionVersionIds\":[null]}")) {
            mockMvc.perform(post("/internal/learning-content/questions/difficulties")
                            .contentType(MediaType.APPLICATION_JSON).content(payload))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(difficulties);
    }

    private static String body(UUID id) {
        return "{\"questionVersionIds\":[\"" + id + "\"]}";
    }
}
