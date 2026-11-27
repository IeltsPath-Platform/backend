package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.usecase.AiGradingReportUseCase;
import com.ieltspath.learning.domain.repository.AiGradingReportRepository;
import com.ieltspath.learning.domain.vo.AiGradingReport;
import com.ieltspath.learning.domain.vo.AiGradingReport.ReportStatus;
import com.ieltspath.learning.domain.vo.AiGradingReport.SubmissionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiGradingReportController.class)
@EnableMethodSecurity
@TestPropertySource(properties = {
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "app.auth.internal-jwt-secret=SGs1cjhXY1lBOEo4am8ycnVJQkNFWFJjSGhVZlNEVXFQMTZIMjlQMXN4bz0="
})
class AiGradingReportControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    CurrentUserProvider currentUser;

    @MockitoBean
    AiGradingReportUseCase useCase;

    @Test
    void examinerCanListReportsWhileAdminAndCustomerAreForbidden() throws Exception {
        var item = new AiGradingReport(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                SubmissionType.WRITING, "Điểm chưa hợp lý", ReportStatus.OPEN, Instant.now());
        when(useCase.list(any(), any(), anyInt(), anyInt()))
                .thenReturn(new AiGradingReportRepository.Page(List.of(item), 0, 20, 1, 1));

        mvc.perform(get("/api/learning/admin/ai-grading-reports")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAMINER"))
                                .jwt(t -> t.subject(UUID.randomUUID().toString()).claim("roles", List.of("EXAMINER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(get("/api/learning/admin/ai-grading-reports")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(t -> t.subject(UUID.randomUUID().toString()).claim("roles", List.of("ADMIN")))))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/learning/admin/ai-grading-reports")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                                .jwt(t -> t.subject(UUID.randomUUID().toString()).claim("roles", List.of("CUSTOMER")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanSubmitReportAndExaminerCannot() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(userId);
        var saved = new AiGradingReport(UUID.randomUUID(), userId, submissionId,
                SubmissionType.SPEAKING, "Lỗi chấm phát âm", ReportStatus.OPEN, Instant.now());
        when(useCase.report(userId, submissionId, SubmissionType.SPEAKING, "Lỗi chấm phát âm"))
                .thenReturn(new AiGradingReportRepository.SaveResult(saved, true));

        mvc.perform(post("/api/learning/ai-grading-reports")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                                .jwt(t -> t.subject(userId.toString()).claim("roles", List.of("CUSTOMER"))))
                        .contentType("application/json")
                        .content("{\"submissionId\":\"" + submissionId + "\",\"submissionType\":\"SPEAKING\",\"reason\":\"Lỗi chấm phát âm\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.submissionType").value("SPEAKING"));

        mvc.perform(post("/api/learning/ai-grading-reports")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAMINER"))
                                .jwt(t -> t.subject(userId.toString()).claim("roles", List.of("EXAMINER"))))
                        .contentType("application/json")
                        .content("{\"submissionId\":\"" + submissionId + "\",\"submissionType\":\"SPEAKING\",\"reason\":\"Lỗi chấm phát âm\"}"))
                .andExpect(status().isForbidden());
    }
}
