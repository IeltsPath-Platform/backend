package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.WritingSubmissionResult;
import com.ieltspath.learning.application.service.WritingSettings;
import com.ieltspath.learning.application.usecase.SubmitFreeWritingToExaminerUseCase;
import com.ieltspath.learning.application.usecase.SubmitFreeWritingUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FreeWritingController.class, properties = {"spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@Import(CommonSecurityAutoConfiguration.class)
class FreeWritingToExaminerControllerTest {
    static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean SubmitFreeWritingUseCase submitFree;
    @MockitoBean SubmitFreeWritingToExaminerUseCase submitToExaminer;
    @MockitoBean WritingSettings settings;

    final UUID learner = UUID.randomUUID();
    final UUID request = UUID.randomUUID();
    final UUID version = UUID.randomUUID();
    final UUID question = UUID.randomUUID();
    final UUID examiner = UUID.randomUUID();
    final UUID essay = UUID.randomUUID();

    private String body(String examinerId) {
        return "{\"requestId\":\"" + request + "\",\"packageVersionId\":\"" + version + "\",\"questionVersionId\":\""
                + question + "\",\"essayText\":\"My essay\"" + (examinerId == null ? "" : ",\"examinerId\":\"" + examinerId + "\"") + "}";
    }

    @Test
    void theEssayGoesToTheChosenExaminerAndTheLearnerGetsItBackAsWaiting() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(submitToExaminer.execute(learner, version, question, request, "My essay", examiner))
                .thenReturn(new WritingSubmissionResult(essay, "AWAITING_EXAMINER", "TASK_1", 2, null, null, null, null,
                        null, null, null, "My essay"));

        mvc.perform(post("/api/learning/free-writing/examiner-submissions").with(user("l").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body(examiner.toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.submissionId").value(essay.toString()))
                .andExpect(jsonPath("$.status").value("AWAITING_EXAMINER"));
    }

    @Test
    void anEssayWhoseLastEssayStillWaitsIsRefusedWithTheWaitingEssayToOpen() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(submitToExaminer.execute(learner, version, question, request, "My essay", examiner))
                .thenThrow(new LearningRequestException(409, "EXAMINER_REVIEW_PENDING", "already sent", essay));

        mvc.perform(post("/api/learning/free-writing/examiner-submissions").with(user("l").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body(examiner.toString())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EXAMINER_REVIEW_PENDING"))
                .andExpect(jsonPath("$.submissionId").value(essay.toString()));
    }

    @Test
    void theLearnerCanAskWhetherThePromptStillWaitsForItsExaminer() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(submitToExaminer.pending(learner, question)).thenReturn(java.util.Optional.of(essay));

        mvc.perform(get("/api/learning/free-writing/examiner-submissions/pending").param("questionVersionId", question.toString())
                        .with(user("l").roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pending").value(true))
                .andExpect(jsonPath("$.submissionId").value(essay.toString()));

        when(submitToExaminer.pending(learner, question)).thenReturn(java.util.Optional.empty());
        mvc.perform(get("/api/learning/free-writing/examiner-submissions/pending").param("questionVersionId", question.toString())
                        .with(user("l").roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pending").value(false))
                .andExpect(jsonPath("$.submissionId").doesNotExist());
    }

    @Test
    void whenAssessmentCannotBeReachedTheQuestionAnswersNotPendingAndSendingChecksAgain() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(submitToExaminer.pending(learner, question))
                .thenThrow(new LearningRequestException(503, "HUMAN_REVIEW_UNAVAILABLE", "down"));

        mvc.perform(get("/api/learning/free-writing/examiner-submissions/pending").param("questionVersionId", question.toString())
                        .with(user("l").roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pending").value(false));
    }

    @Test
    void anEssayWithoutAnExaminerIsRefusedBeforeAnythingHappens() throws Exception {
        mvc.perform(post("/api/learning/free-writing/examiner-submissions").with(user("l").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body(null)))
                .andExpect(status().isUnprocessableEntity());

        verifyNoInteractions(submitToExaminer);
    }

    @Test
    void noPremiumCreditAndAnUnavailableExaminerKeepTheirCodes() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(submitToExaminer.execute(learner, version, question, request, "My essay", examiner))
                .thenThrow(new LearningRequestException(402, "HUMAN_GRADING_CREDIT_REQUIRED", "none"))
                .thenThrow(new LearningRequestException(409, "EXAMINER_UNAVAILABLE", "full"));

        mvc.perform(post("/api/learning/free-writing/examiner-submissions").with(user("l").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body(examiner.toString())))
                .andExpect(status().isPaymentRequired()).andExpect(jsonPath("$.code").value("HUMAN_GRADING_CREDIT_REQUIRED"));
        mvc.perform(post("/api/learning/free-writing/examiner-submissions").with(user("l").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body(examiner.toString())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EXAMINER_UNAVAILABLE"));
    }

    @Test
    void anonymousCallersAreRejected() throws Exception {
        mvc.perform(post("/api/learning/free-writing/examiner-submissions").contentType(MediaType.APPLICATION_JSON)
                        .content(body(examiner.toString())))
                .andExpect(status().isUnauthorized());
    }
}
