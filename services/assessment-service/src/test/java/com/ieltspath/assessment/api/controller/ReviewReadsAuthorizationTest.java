package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.port.ExaminerQueueStore;
import com.ieltspath.assessment.application.service.AiEvaluationReadService;
import com.ieltspath.assessment.application.service.ExaminerQaService;
import com.ieltspath.assessment.application.usecase.ClaimGradingJobUseCase;
import com.ieltspath.assessment.application.usecase.GetJobEssayUseCase;
import com.ieltspath.assessment.application.usecase.ListGradingQueueUseCase;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The new read routes of the review screens: only a content author may call them. */
@WebMvcTest(controllers = {ExaminerQaController.class, ExaminerQualityController.class,
        AiEvaluationReadController.class}, properties = "spring.cloud.config.enabled=false")
@Import(GradingControllerTest.MethodSecurityConfig.class)
class ReviewReadsAuthorizationTest {
    static final String[] ROUTES = {
            "/api/assessments/qa-reviews/mine",
            "/api/assessments/admin/examiner-quality",
            "/api/assessments/admin/ai-gold-essays",
            "/api/assessments/admin/ai-evaluations",
            "/api/assessments/admin/ai-prompt-activations"};

    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean ListGradingQueueUseCase queue;
    @MockitoBean ClaimGradingJobUseCase claim;
    @MockitoBean GetJobEssayUseCase essay;
    @MockitoBean ExaminerQaService qa;
    @MockitoBean AiEvaluationReadService reads;

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "EXAMINER", "ADMIN"})
    void everyOtherRoleIsRefused(String role) throws Exception {
        for (String route : ROUTES) {
            mvc.perform(get(route).with(org.springframework.security.test.web.servlet.request
                    .SecurityMockMvcRequestPostProcessors.user("someone").roles(role)))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(queue, qa, reads);
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void mineIsTheCallersOwnClaimedEssays() throws Exception {
        UUID reviewer = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(reviewer);
        when(queue.claimedQaBy(eq(reviewer), eq(0), eq(20)))
                .thenReturn(new ExaminerQueueStore.QueuePage(List.of(), 0, 20, 0));

        mvc.perform(get("/api/assessments/qa-reviews/mine")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray()).andExpect(jsonPath("$.totalElements").value(0));
        verify(queue).claimedQaBy(reviewer, 0, 20);
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void listsPassTheirFiltersAndPagingOn() throws Exception {
        when(reads.goldEssays(any(), any(Integer.class), any(Integer.class)))
                .thenReturn(new AiEvaluationReadService.Page<>(List.of(), 1, 5, 0));
        when(reads.evaluations(any(), any(Integer.class), any(Integer.class)))
                .thenReturn(new AiEvaluationReadService.Page<>(List.of(), 2, 7, 0));
        when(reads.activations()).thenReturn(List.of(
                new AiEvaluationReadService.ActivationRow("TASK_1", null, null, null),
                new AiEvaluationReadService.ActivationRow("TASK_2", "v3", UUID.randomUUID(), null)));

        mvc.perform(get("/api/assessments/admin/ai-gold-essays?task=TASK_1&page=1&size=5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(5));
        verify(reads).goldEssays("TASK_1", 1, 5);
        mvc.perform(get("/api/assessments/admin/ai-evaluations?page=2&size=7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(2));
        verify(reads).evaluations(null, 2, 7);
        mvc.perform(get("/api/assessments/admin/ai-prompt-activations"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].promptVersion").isEmpty())
                .andExpect(jsonPath("$[1].promptVersion").value("v3"));
    }
}
