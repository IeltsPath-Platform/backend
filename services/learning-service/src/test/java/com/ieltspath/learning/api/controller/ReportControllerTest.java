package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.result.LearnerReportResult;
import com.ieltspath.learning.application.usecase.GetLearnerReportUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReportController.class, properties = {"spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@Import(CommonSecurityAutoConfiguration.class)
class ReportControllerTest {
    static final byte[] KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean GetLearnerReportUseCase report;
    final UUID learner = UUID.randomUUID();
    final UUID other = UUID.randomUUID();

    private LearnerReportResult sample(UUID userId) {
        UUID point = UUID.randomUUID();
        return new LearnerReportResult(userId, Instant.parse("2026-10-16T10:00:00Z"), List.of(
                new LearnerReportResult.KnowledgePointReport(point, "R60_TFNG_F_VS_NG", "False vs not given", "READING",
                        new BigDecimal("6.0"), new LearnerReportResult.Score(0.72, "CONSOLIDATE", 9),
                        List.of(new LearnerReportResult.ErrorEntry("TFNG_NG_AS_FALSE", 3)),
                        List.of(new LearnerReportResult.EvidenceEntry(UUID.randomUUID(), false, 41000L, "practice_set",
                                "TFNG_NG_AS_FALSE", Instant.parse("2026-10-15T08:00:00Z"))),
                        new LearnerReportResult.Action("REVIEW_REQUIRED", UUID.randomUUID(), null))));
    }

    @Test
    void aLearnerReadsOnlyTheirOwnReportWhateverTheyPutInTheRequest() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(report.execute(learner)).thenReturn(sample(learner));

        mvc.perform(get("/api/learning/report").param("userId", other.toString())
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(learner.toString()))
                .andExpect(jsonPath("$.knowledgePoints[0].score.tier").value("CONSOLIDATE"))
                .andExpect(jsonPath("$.knowledgePoints[0].errors[0].code").value("TFNG_NG_AS_FALSE"))
                .andExpect(jsonPath("$.knowledgePoints[0].evidence[0].durationMs").value(41000))
                .andExpect(jsonPath("$.knowledgePoints[0].action.type").value("REVIEW_REQUIRED"));
        verify(report).execute(learner);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "EXAMINER"})
    void staffReadAnyLearnersReport(String role) throws Exception {
        when(report.execute(other)).thenReturn(sample(other));

        mvc.perform(get("/api/learning/admin/learners/" + other + "/report").with(user("staff").roles(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(other.toString()));
        verify(report).execute(other);
    }

    @Test
    void aCustomerCannotCallTheStaffReport() throws Exception {
        mvc.perform(get("/api/learning/admin/learners/" + other + "/report").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(report);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "EXAMINER", "CONTENT_AUTHOR"})
    void staffCannotUseTheLearnerRoute(String role) throws Exception {
        mvc.perform(get("/api/learning/report").with(user("staff").roles(role))).andExpect(status().isForbidden());
        verifyNoInteractions(report);
    }

    @Test
    void contentAuthorsCannotReadLearnerReports() throws Exception {
        mvc.perform(get("/api/learning/admin/learners/" + other + "/report")
                        .with(user("author").roles("CONTENT_AUTHOR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(report);
    }

    @Test
    void unauthenticatedCallersAreRejected() throws Exception {
        mvc.perform(get("/api/learning/report")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/learning/admin/learners/" + other + "/report")).andExpect(status().isUnauthorized());
    }
}
