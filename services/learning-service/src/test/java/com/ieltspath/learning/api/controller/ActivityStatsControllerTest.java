package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.learning.application.usecase.GetActivityStatsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ActivityStatsController.class, properties = {"spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@Import(CommonSecurityAutoConfiguration.class)
class ActivityStatsControllerTest {
    static final byte[] KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired MockMvc mvc;
    @MockitoBean GetActivityStatsUseCase stats;

    @Test
    void anAdminGetsTheStatistics() throws Exception {
        LocalDate from = LocalDate.of(2026, 3, 2);
        LocalDate to = LocalDate.of(2026, 3, 8);
        when(stats.execute(from, to)).thenReturn(new GetActivityStatsUseCase.Result(from, to,
                List.of(new GetActivityStatsUseCase.Day(from, 1, 1, 1)), List.of()));

        mvc.perform(get("/api/learning/admin/activity-stats").param("from", "2026-03-02").param("to", "2026-03-08")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.daily[0].dau").value(1));

        verify(stats).execute(eq(from), eq(to));
    }

    @Test
    void aLearnerCannotSeeTheStatistics() throws Exception {
        mvc.perform(get("/api/learning/admin/activity-stats").param("from", "2026-03-02").param("to", "2026-03-08")
                        .with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isForbidden());

        verify(stats, org.mockito.Mockito.never()).execute(any(), any());
    }
}
