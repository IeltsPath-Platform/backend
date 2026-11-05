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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
                new HumanReviewStore.ReviewResult(reviewId, "PENDING", null, null, null, null, null, UUID.randomUUID()));
        String body = """
                {"requestId":"%s","learnerId":"%s","sourceSubmissionId":"%s","source":"LESSON",
                 "task":"TASK_1","promptSnapshot":{"stem":"Describe the chart."},"essay":"Essay text.",
                 "rubric":{"code":"IELTS_WRITING_TASK_1","version":1},"examinerId":"%s"}
                """.formatted(requestId, learnerId, UUID.randomUUID(), UUID.randomUUID());

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
                 "rubric":{"code":"IELTS_WRITING_TASK_1","version":1},"examinerId":"%s"}
                """.formatted(UUID.randomUUID(), other, UUID.randomUUID(), UUID.randomUUID());

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

    @Test
    @WithMockUser(roles = "EXAMINER")
    void passesTheMarkedErrorsToTheService() throws Exception {
        UUID examinerId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(examinerId);
        mvc.perform(post("/api/assessments/grading-jobs/{id}/human-review", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"band":6.5,"criteria":[{"criterion":"GRA","band":6.0,"comment":"Tense slips."}],
                                 "errorCodes":["TENSE"],"feedback":"Check tenses.",
                                 "annotations":[{"excerpt":"have rose","start":10,"end":19,"errorCode":"TENSE",
                                                 "suggestion":"have risen"}]}
                                """))
                .andExpect(status().isNoContent());
        var command = org.mockito.ArgumentCaptor.forClass(
                com.ieltspath.assessment.application.command.CompleteHumanReviewCommand.class);
        verify(service).complete(org.mockito.ArgumentMatchers.eq(jobId), org.mockito.ArgumentMatchers.eq(examinerId),
                command.capture());
        org.junit.jupiter.api.Assertions.assertEquals(
                new com.ieltspath.assessment.application.command.CompleteHumanReviewCommand.Annotation(
                        "have rose", 10, 19, "TENSE", "have risen"), command.getValue().annotations().getFirst());
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void rejectsAMarkedErrorWithoutAnExcerptOrWithAnEmptyRange() throws Exception {
        for (String annotation : new String[] {
                "{\"excerpt\":\"\",\"start\":1,\"end\":2,\"errorCode\":\"TENSE\",\"suggestion\":\"x\"}",
                "{\"excerpt\":\"a\",\"start\":1,\"end\":0,\"errorCode\":\"TENSE\",\"suggestion\":\"x\"}",
                "{\"excerpt\":\"a\",\"start\":1,\"end\":2,\"errorCode\":\"\",\"suggestion\":\"x\"}"}) {
            mvc.perform(post("/api/assessments/grading-jobs/{id}/human-review", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"band\":6.5,\"criteria\":[{\"criterion\":\"GRA\",\"band\":6.0,\"comment\":\"c\"}],"
                                    + "\"errorCodes\":[],\"feedback\":\"f\",\"annotations\":[" + annotation + "]}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void examinerSavesAndReadsTheDraft() throws Exception {
        UUID examinerId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(examinerId);
        mvc.perform(put("/api/assessments/grading-jobs/{id}/human-review/draft", jobId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"band\":5.5}"))
                .andExpect(status().isNoContent());
        verify(service).saveDraft(org.mockito.ArgumentMatchers.eq(jobId), org.mockito.ArgumentMatchers.eq(examinerId),
                org.mockito.ArgumentMatchers.any(com.fasterxml.jackson.databind.JsonNode.class));

        when(service.draft(jobId, examinerId)).thenReturn(java.util.Optional.of(new HumanReviewStore.Draft(
                json.readTree("{\"band\":5.5}"), java.time.Instant.parse("2026-10-22T01:00:00Z"))));
        mvc.perform(get("/api/assessments/grading-jobs/{id}/human-review/draft", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.band").value(5.5))
                .andExpect(jsonPath("$.savedAt").exists());
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void readingADraftThatWasNeverSavedIsEmpty() throws Exception {
        UUID examinerId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(examinerId);
        when(service.draft(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(examinerId)))
                .thenReturn(java.util.Optional.empty());
        mvc.perform(get("/api/assessments/grading-jobs/{id}/human-review/draft", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerCannotSaveOrReadTheDraft() throws Exception {
        mvc.perform(put("/api/assessments/grading-jobs/{id}/human-review/draft", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/assessments/grading-jobs/{id}/human-review/draft", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
