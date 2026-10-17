package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.usecase.RequestHumanGradingUseCase;
import com.ieltspath.assessment.domain.exception.HumanGradingCreditInsufficientException;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HumanGradingRequestController.class, properties = "spring.cloud.config.enabled=false")
@Import(GradingControllerTest.MethodSecurityConfig.class)
class HumanGradingRequestControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean RequestHumanGradingUseCase request;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerCanRequestHumanGrading() throws Exception {
        UUID user = UUID.randomUUID();
        UUID attempt = UUID.randomUUID();
        UUID item = UUID.randomUUID();
        UUID submission = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(user);
        when(request.execute(user, attempt, item))
                .thenReturn(new RequestHumanGradingUseCase.RequestResult(submission, "QUEUED"));

        mvc.perform(post("/api/assessments/attempts/{attemptId}/items/{itemId}/human-grading", attempt, item))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.submissionId").value(submission.toString()))
                .andExpect(jsonPath("$.status").value("QUEUED"));
        verify(request).execute(user, attempt, item);
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void examinerCannotRequestLearnerCredit() throws Exception {
        mvc.perform(post("/api/assessments/attempts/{attemptId}/items/{itemId}/human-grading",
                        UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(request);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void insufficientCreditsReturnPaymentRequired() throws Exception {
        UUID user = UUID.randomUUID();
        UUID attempt = UUID.randomUUID();
        UUID item = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(user);
        when(request.execute(user, attempt, item)).thenThrow(new HumanGradingCreditInsufficientException());

        mvc.perform(post("/api/assessments/attempts/{attemptId}/items/{itemId}/human-grading", attempt, item))
                .andExpect(status().isPaymentRequired());
    }
}
