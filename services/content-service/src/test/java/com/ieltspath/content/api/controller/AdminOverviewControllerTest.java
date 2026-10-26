package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.content.application.result.ContentOverviewResult;
import com.ieltspath.content.application.usecase.GetContentOverviewUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.Map;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminOverviewController.class)
@Import(CommonSecurityAutoConfiguration.class)
class AdminOverviewControllerTest {
    @DynamicPropertySource
    static void auth(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(new byte[32]));
        registry.add("app.auth.internal-jwt-issuer", () -> "urn:code-base:api-gateway");
    }

    @Autowired MockMvc mvc;
    @MockitoBean GetContentOverviewUseCase overview;

    private ContentOverviewResult counts() {
        return new ContentOverviewResult(
                new ContentOverviewResult.KnowledgePointCounts(70, Map.of("ACTIVE", 57L, "INACTIVE", 13L),
                        Map.of("READING", 12L), Map.of("B1", 20L)),
                12, 4, Map.of("DRAFT", 2L, "PUBLISHED", 9L, "ARCHIVED", 0L));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void aLearnerCannotReadTheOverview() throws Exception {
        mvc.perform(get("/api/content/admin/overview")).andExpect(status().isForbidden());
        verifyNoInteractions(overview);
    }

    @Test
    @WithMockUser(roles = {"CONTENT_AUTHOR"})
    void anAuthorReadsEveryCount() throws Exception {
        when(overview.execute()).thenReturn(counts());

        mvc.perform(get("/api/content/admin/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.knowledgePoints.total").value(70))
                .andExpect(jsonPath("$.knowledgePoints.byStatus.ACTIVE").value(57))
                .andExpect(jsonPath("$.knowledgePoints.byCefr.B1").value(20))
                .andExpect(jsonPath("$.activeKnowledgePointsWithoutPublishedQuestion").value(12))
                .andExpect(jsonPath("$.draftQuestionVersions").value(4))
                .andExpect(jsonPath("$.lessons.PUBLISHED").value(9));
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void anExaminerMayRead() throws Exception {
        when(overview.execute()).thenReturn(counts());

        mvc.perform(get("/api/content/admin/overview")).andExpect(status().isOk());
    }
}
