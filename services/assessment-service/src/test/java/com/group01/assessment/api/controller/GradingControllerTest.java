package com.group01.assessment.api.controller;

import com.group01.assessment.application.command.FinalizeAssessmentResultCommand;
import com.group01.assessment.application.command.SaveGradingDetailsCommand;
import com.group01.assessment.application.result.AssessmentResultResult;
import com.group01.assessment.application.usecase.CreateAssessmentResultUseCase;
import com.group01.assessment.application.usecase.FinalizeAssessmentResultUseCase;
import com.group01.assessment.application.usecase.SaveAssessmentResultDetailsUseCase;
import com.group01.assessment.domain.entity.AssessmentResult;
import com.group01.assessment.domain.vo.QualitativeJudgment;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GradingController.class, properties = "spring.cloud.config.enabled=false")
@Import(GradingControllerTest.MethodSecurityConfig.class)
class GradingControllerTest {

    /** Method security as the runtime enables it; JWT is replaced by {@link WithMockUser}. */
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

    @Autowired MockMvc mockMvc;
    @MockitoBean CreateAssessmentResultUseCase createResult;
    @MockitoBean SaveAssessmentResultDetailsUseCase saveDetails;
    @MockitoBean FinalizeAssessmentResultUseCase finalizeResult;

    private final UUID attemptId = UUID.randomUUID();
    private final UUID resultId = UUID.randomUUID();
    private final UUID attemptItemId = UUID.randomUUID();
    private final UUID knowledgePointId = UUID.randomUUID();

    private MockHttpServletRequestBuilder openVersion(String body) {
        return post("/api/assessments/grading/attempts/{attemptId}/results", attemptId)
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder saveDetailsRequest(String body) {
        return put("/api/assessments/grading/results/{resultId}/details", resultId)
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder finalizeRequest() {
        return post("/api/assessments/grading/results/{resultId}/finalize", resultId);
    }

    private String detailsBody() {
        return """
                {"overallBand":6.5,
                 "itemResults":[{"attemptItemId":"%s","score":1,"maxScore":1,"correct":true}],
                 "knowledgeJudgments":[{"attemptItemId":"%s","knowledgePointId":"%s","judgment":"PASS"}]}
                """.formatted(attemptItemId, attemptItemId, knowledgePointId);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerIsForbiddenOnEveryGradingEndpoint() throws Exception {
        mockMvc.perform(openVersion("{}")).andExpect(status().isForbidden());
        mockMvc.perform(saveDetailsRequest(detailsBody())).andExpect(status().isForbidden());
        mockMvc.perform(finalizeRequest()).andExpect(status().isForbidden());

        verifyNoInteractions(createResult, saveDetails, finalizeResult);
    }

    @Test
    void anonymousCallerIsRejected() throws Exception {
        mockMvc.perform(finalizeRequest()).andExpect(status().isForbidden());

        verifyNoInteractions(finalizeResult);
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void examinerOpensTheNextResultVersion() throws Exception {
        when(createResult.executeForGrader(attemptId, 7.0)).thenReturn(draft(2, 7.0));

        mockMvc.perform(openVersion("{\"overallBand\":7.0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultVersion").value(2))
                .andExpect(jsonPath("$.status").value(AssessmentResult.DRAFT));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void openingAVersionAcceptsAnEmptyBody() throws Exception {
        when(createResult.executeForGrader(eq(attemptId), isNull())).thenReturn(draft(1, null));

        mockMvc.perform(post("/api/assessments/grading/attempts/{attemptId}/results", attemptId))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void examinerSavesGradingDetails() throws Exception {
        mockMvc.perform(saveDetailsRequest(detailsBody())).andExpect(status().isNoContent());

        ArgumentCaptor<SaveGradingDetailsCommand> command = ArgumentCaptor.forClass(SaveGradingDetailsCommand.class);
        verify(saveDetails).executeForGrader(command.capture());
        assertEquals(resultId, command.getValue().resultId());
        assertEquals(6.5, command.getValue().overallBand());
        assertEquals(attemptItemId, command.getValue().itemResults().get(0).attemptItemId());
        assertEquals("{}", command.getValue().itemResults().get(0).feedbackSnapshot());
        assertEquals(QualitativeJudgment.PASS, command.getValue().knowledgeJudgments().get(0).judgment());
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void omittedBandIsSentAsNullSoTheGraderClearsIt() throws Exception {
        mockMvc.perform(saveDetailsRequest("{}")).andExpect(status().isNoContent());

        ArgumentCaptor<SaveGradingDetailsCommand> command = ArgumentCaptor.forClass(SaveGradingDetailsCommand.class);
        verify(saveDetails).executeForGrader(command.capture());
        assertNull(command.getValue().overallBand());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminFinalizesAResult() throws Exception {
        when(finalizeResult.execute(new FinalizeAssessmentResultCommand(resultId))).thenReturn(
                new AssessmentResultResult(resultId, attemptId, 1, AssessmentResult.COMPLETED, 6.5, Instant.now()));

        mockMvc.perform(finalizeRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(AssessmentResult.COMPLETED))
                .andExpect(jsonPath("$.overallBand").value(6.5));
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void bandOutsideTheIeltsScaleIsRejected() throws Exception {
        mockMvc.perform(openVersion("{\"overallBand\":9.5}")).andExpect(status().isBadRequest());
        mockMvc.perform(saveDetailsRequest("{\"overallBand\":-1}")).andExpect(status().isBadRequest());

        verify(createResult, never()).executeForGrader(any(), anyDouble());
        verifyNoInteractions(saveDetails);
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void itemResultWithoutScoresIsRejected() throws Exception {
        mockMvc.perform(saveDetailsRequest("""
                        {"itemResults":[{"attemptItemId":"%s","correct":true}]}
                        """.formatted(attemptItemId)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(saveDetails);
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void judgmentWithoutAKnowledgePointIsRejected() throws Exception {
        mockMvc.perform(saveDetailsRequest("""
                        {"knowledgeJudgments":[{"attemptItemId":"%s","judgment":"PASS"}]}
                        """.formatted(attemptItemId)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(saveDetails);
    }

    private AssessmentResultResult draft(int version, Double band) {
        return new AssessmentResultResult(UUID.randomUUID(), attemptId, version, AssessmentResult.DRAFT, band, null);
    }
}
