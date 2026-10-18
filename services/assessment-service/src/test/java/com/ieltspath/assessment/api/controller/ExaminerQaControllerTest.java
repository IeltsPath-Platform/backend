package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.port.ExaminerQueueStore;
import com.ieltspath.assessment.application.service.ExaminerQaService;
import com.ieltspath.assessment.application.usecase.ClaimGradingJobUseCase;
import com.ieltspath.assessment.application.usecase.GetJobEssayUseCase;
import com.ieltspath.assessment.application.usecase.ListGradingQueueUseCase;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ExaminerQaController.class, properties = "spring.cloud.config.enabled=false")
@Import(GradingControllerTest.MethodSecurityConfig.class)
class ExaminerQaControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean ListGradingQueueUseCase queue;
    @MockitoBean ClaimGradingJobUseCase claim;
    @MockitoBean GetJobEssayUseCase essay;
    @MockitoBean ExaminerQaService qa;

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void contentAuthorCanSeeOnlyQaQueueAndSubmitBlindReview() throws Exception {
        UUID reviewer = UUID.randomUUID();
        UUID job = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(reviewer);
        when(queue.execute("HUMAN", "QUEUED", 0, 20, "QA_SAMPLE"))
                .thenReturn(new ExaminerQueueStore.QueuePage(List.of(), 0, 20, 0));

        mvc.perform(get("/api/assessments/qa-reviews/queue"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        verify(queue).execute("HUMAN", "QUEUED", 0, 20, "QA_SAMPLE");
        mvc.perform(post("/api/assessments/qa-reviews").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gradingJobId\":\"" + job + "\",\"reviewerBand\":6.5,\"reviewerCriteria\":{\"TR\":6.0},\"note\":\"review\"}"))
                .andExpect(status().isNoContent());
        verify(qa).submitReview(eq(job), eq(reviewer), eq(new java.math.BigDecimal("6.5")),
                eq("{\"TR\":6.0}"), eq("review"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnersCannotAccessMentorQualityReviewQueue() throws Exception {
        mvc.perform(get("/api/assessments/qa-reviews/queue")).andExpect(status().isForbidden());
        mvc.perform(post("/api/assessments/qa-reviews").contentType(MediaType.APPLICATION_JSON)
                .content("{\"gradingJobId\":\"" + UUID.randomUUID()
                        + "\",\"reviewerBand\":6.0,\"reviewerCriteria\":{\"TR\":6.0}}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(qa, queue);
    }
}
