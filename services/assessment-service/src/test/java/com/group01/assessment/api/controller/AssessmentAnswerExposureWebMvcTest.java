package com.group01.assessment.api.controller;

import com.group01.assessment.application.result.AttemptStructureResult;
import com.group01.assessment.application.usecase.*;
import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AssessmentAttemptController.class, AssessmentResultController.class},
        properties = "spring.cloud.config.enabled=false")
@Import(AssessmentAnswerExposureWebMvcTest.TestSecurityConfig.class)
class AssessmentAnswerExposureWebMvcTest {
    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                    .build();
        }
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean StartAssessmentAttemptUseCase startAttempt;
    @MockitoBean GetAssessmentAttemptUseCase getAttempt;
    @MockitoBean GetAttemptStructureUseCase getStructure;
    @MockitoBean SubmitAssessmentAttemptUseCase submitAttempt;
    @MockitoBean ExpireAssessmentAttemptUseCase expireAttempt;
    @MockitoBean SaveAttemptResponseUseCase saveResponse;
    @MockitoBean GetAssessmentResultUseCase getResult;

    private final UUID userId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void ownerSeesStructureWithoutAnswerSnapshot() throws Exception {
        when(currentUser.requireUserId()).thenReturn(userId);
        when(getStructure.execute(userId, attemptId)).thenReturn(new AttemptStructureResult(List.of(
                new AttemptStructureResult.Section(sectionId, UUID.randomUUID(), 1, "{\"title\":\"Reading\"}",
                        List.of(new AttemptStructureResult.Item(itemId, UUID.randomUUID(), 1,
                                "{\"stem\":\"Question\"}", "{\"kp\":\"KP1\"}"))))));

        mockMvc.perform(get("/api/assessments/attempts/{id}/structure", attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[0].id").value(sectionId.toString()))
                .andExpect(jsonPath("$.sections[0].items[0].id").value(itemId.toString()))
                .andExpect(jsonPath("$.sections[0].items[0].questionSnapshot").value("{\"stem\":\"Question\"}"))
                .andExpect(jsonPath("$.sections[0].items[0].knowledgeSnapshot").value("{\"kp\":\"KP1\"}"))
                .andExpect(jsonPath("$.sections[0].items[0].answerSnapshot").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerCannotOpenResultVersion() throws Exception {
        mockMvc.perform(post("/api/assessments/attempts/{id}/result", attemptId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"overallBand\":6.0}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
