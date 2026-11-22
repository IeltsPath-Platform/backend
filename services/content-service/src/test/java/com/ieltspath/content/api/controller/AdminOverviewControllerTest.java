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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyCollection;
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
                12, 4, Map.of("DRAFT", 2L, "PUBLISHED", 9L, "ARCHIVED", 0L),
                List.of(new ContentOverviewResult.CoverageCell("READING", "B1", 20, 5, 3)),
                Map.of("DRAFT", 4L, "PUBLISHED", 30L), Map.of("PUBLISHED", 6L), 2, 14);
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
                .andExpect(jsonPath("$.lessons.PUBLISHED").value(9))
                .andExpect(jsonPath("$.coverage[0].skill").value("READING"))
                .andExpect(jsonPath("$.coverage[0].withoutPublishedQuestion").value(5))
                .andExpect(jsonPath("$.coverage[0].withOnePublishedQuestion").value(3))
                .andExpect(jsonPath("$.questionVersions.PUBLISHED").value(30))
                .andExpect(jsonPath("$.packages.PUBLISHED").value(6))
                .andExpect(jsonPath("$.staleDraftQuestionVersions").value(2))
                .andExpect(jsonPath("$.staleDraftAfterDays").value(14));
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void anExaminerMayRead() throws Exception {
        when(overview.execute()).thenReturn(counts());

        mvc.perform(get("/api/content/admin/overview")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void aLearnerCannotReadQuestionLabels() throws Exception {
        mvc.perform(get("/api/content/admin/overview/question-labels").param("ids", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(overview);
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void anAuthorGetsTheLabelOfEachQuestionVersionAskedFor() throws Exception {
        UUID version = UUID.randomUUID();
        UUID question = UUID.randomUUID();
        when(overview.questionLabels(anyCollection())).thenReturn(
                List.of(new ContentOverviewResult.QuestionLabel(version, question, "Choose the correct heading", "READING", "MULTIPLE_CHOICE")));

        mvc.perform(get("/api/content/admin/overview/question-labels").param("ids", version.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questionVersionId").value(version.toString()))
                .andExpect(jsonPath("$[0].questionId").value(question.toString()))
                .andExpect(jsonPath("$[0].label").value("Choose the correct heading"));
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void anIdThatIsNotAUuidIsRefused() throws Exception {
        mvc.perform(get("/api/content/admin/overview/question-labels").param("ids", "not-a-uuid"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(overview);
    }
}