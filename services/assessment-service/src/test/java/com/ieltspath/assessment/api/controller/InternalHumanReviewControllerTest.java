package com.ieltspath.assessment.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.api.dto.request.CompleteHumanReviewRequest;
import com.ieltspath.assessment.application.port.HumanReviewStore;
import com.ieltspath.assessment.application.service.HumanReviewService;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
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

import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {InternalHumanReviewController.class, HumanReviewGradingController.class},
        properties = "spring.cloud.config.enabled=false")
@Import(InternalHumanReviewControllerTest.MethodSecurityConfig.class)
class InternalHumanReviewControllerTest {
    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                    .build();
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean HumanReviewService service;
    @MockitoBean CurrentUserProvider currentUser;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createsAndReadsPendingReviewWithoutGradeFields() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID learnerId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(learnerId);
        when(service.create(org.mockito.ArgumentMatchers.any())).thenReturn(
                new HumanReviewStore.Review(requestId, reviewId, "PENDING"));
        when(service.get(reviewId, learnerId)).thenReturn(
                new HumanReviewStore.ReviewResult(reviewId, "PENDING", null, null, null, null));
        String body = """
                {"requestId":"%s","learnerId":"%s","sourceSubmissionId":"%s","source":"LESSON",
                 "task":"TASK_1","promptSnapshot":{"stem":"Describe the chart."},"essay":"Essay text.",
                 "rubric":{"code":"IELTS_WRITING_TASK_1","version":1}}
                """.formatted(requestId, learnerId, UUID.randomUUID());

        mvc.perform(post("/internal/assessments/human-reviews").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.reviewSubmissionId").value(reviewId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.band").doesNotExist());
        mvc.perform(get("/internal/assessments/human-reviews/{id}", reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.criteria").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void readsAndCreatesOnlyForTheAuthenticatedLearner() throws Exception {
        UUID caller = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(caller);
        when(service.get(reviewId, caller)).thenThrow(new AssessmentNotFoundException("Human review not found"));
        String body = """
                {"requestId":"%s","learnerId":"%s","sourceSubmissionId":"%s","source":"LESSON",
                 "task":"TASK_1","promptSnapshot":{"stem":"Describe the chart."},"essay":"Essay text.",
                 "rubric":{"code":"IELTS_WRITING_TASK_1","version":1}}
                """.formatted(UUID.randomUUID(), other, UUID.randomUUID());

        mvc.perform(get("/internal/assessments/human-reviews/{id}", reviewId)
                        .param("learnerId", other.toString()))
                .andExpect(status().isNotFound());
        verify(service).get(reviewId, caller);
        mvc.perform(post("/internal/assessments/human-reviews").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        verify(service, never()).create(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerCannotCompleteGradingJob() throws Exception {
        mvc.perform(post("/api/assessments/grading-jobs/{id}/human-review", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"band":6.5,"criteria":[{"criterion":"TR","band":6.0,"comment":"Improve."}],
                                 "errorCodes":[],"feedback":"Try again."}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void assignedExaminerSubmitsCompleteGrade() throws Exception {
        UUID examinerId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(examinerId);
        String body = """
                {"band":6.5,"criteria":[{"criterion":"TR","band":6.0,"comment":"Improve."}],
                 "errorCodes":["TASK_RESPONSE_UNDERDEVELOPED"],"feedback":"Try again."}
                """;

        mvc.perform(post("/api/assessments/grading-jobs/{id}/human-review", jobId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        verify(service).complete(org.mockito.ArgumentMatchers.eq(jobId), org.mockito.ArgumentMatchers.eq(examinerId),
                org.mockito.ArgumentMatchers.any(com.ieltspath.assessment.application.command.CompleteHumanReviewCommand.class));
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void rejectsBandsOutsideTheIeltsRange() throws Exception {
        mvc.perform(post("/api/assessments/grading-jobs/{id}/human-review", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"band":9.5,"criteria":[{"criterion":"TR","band":6.0,"comment":"Improve."}],
                                 "errorCodes":[],"feedback":"Try again."}
                                """))
                .andExpect(status().isBadRequest());
    }
}
