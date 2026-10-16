package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.usecase.*;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers=GradingJobController.class, properties="spring.cloud.config.enabled=false")
@Import(GradingControllerTest.MethodSecurityConfig.class)
class GradingJobControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean GetGradingJobUseCase getJob;
    @MockitoBean CreateGradingJobUseCase createJob;
    @MockitoBean ListGradingQueueUseCase listQueue;
    @MockitoBean ClaimGradingJobUseCase claimJob;
    @MockitoBean GetJobEssayUseCase getEssay;
    @Test @WithMockUser(roles="EXAMINER") void essayAndQueueUseBlindProjections() throws Exception {
        UUID id=UUID.randomUUID(),examiner=UUID.randomUUID();
        org.mockito.Mockito.when(currentUser.requireUserId()).thenReturn(examiner);
        org.mockito.Mockito.when(getEssay.execute(examiner,id)).thenReturn(new com.ieltspath.assessment.application.port.ExaminerQueueStore.Essay(id,UUID.randomUUID(),UUID.randomUUID(),"Prompt","Essay","TASK_2",null));
        org.mockito.Mockito.when(listQueue.execute("HUMAN","QUEUED",0,20)).thenReturn(new com.ieltspath.assessment.application.port.ExaminerQueueStore.QueuePage(java.util.List.of(),0,20,0));
        mvc.perform(get("/api/assessments/grading-jobs/{id}/essay",id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.essay").value("Essay")).andExpect(jsonPath("$..band").doesNotExist())
                .andExpect(jsonPath("$..llmBand").doesNotExist()).andExpect(jsonPath("$..llmFeedback").doesNotExist());
        mvc.perform(get("/api/assessments/grading-jobs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray()).andExpect(jsonPath("$..llmBand").doesNotExist());
    }
    @Test @WithMockUser(roles="CUSTOMER") void customerCannotListClaimOrReadExaminerEssay() throws Exception {
        UUID id=UUID.randomUUID();
        mvc.perform(get("/api/assessments/grading-jobs")).andExpect(status().isForbidden());
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim",id)).andExpect(status().isForbidden());
        mvc.perform(get("/api/assessments/grading-jobs/{id}/essay",id)).andExpect(status().isForbidden());
    }
}
