package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.content.application.usecase.ListTestSourcesUseCase;
import com.ieltspath.content.application.usecase.SearchPracticeTestsUseCase;
import com.ieltspath.content.application.port.PracticeTestCatalogReader;
import com.ieltspath.content.application.result.PracticeTestCardResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PracticeTestController.class, properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Import(CommonSecurityAutoConfiguration.class)
class PracticeTestControllerTest {
    private static final byte[] INTERNAL_KEY = new byte[32];

    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchPracticeTestsUseCase search;

    @MockitoBean
    private ListTestSourcesUseCase sources;

    @MockitoBean
    private com.ieltspath.content.application.usecase.ListPracticeTestFormatsUseCase formats;

    @MockitoBean
    private com.ieltspath.content.application.usecase.GetWritingTaskUseCase writingTask;

    @MockitoBean
    private com.ieltspath.content.application.usecase.GetSpeakingTaskUseCase speakingTask;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void speakingTaskRouteReturnsThePartAndItsQuestions() throws Exception {
        UUID versionId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        when(speakingTask.execute(versionId, sectionId)).thenReturn(new com.ieltspath.content.application.port
                .SpeakingTaskReader.SpeakingTask(UUID.randomUUID(), versionId, sectionId, "Speaking Practice Test 1",
                "Part 1: Hometown", 1, 300, "Answer each question in about 20-30 seconds.", List.of(
                new com.ieltspath.content.application.port.SpeakingTaskReader.Question(questionId, 1,
                        "Where is your hometown?", 30))));

        var response = mockMvc.perform(get("/api/content/practice-tests/{v}/sections/{s}/speaking-task", versionId,
                        sectionId))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.title")
                        .value("Part 1: Hometown"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.questions[0].prompt").value("Where is your hometown?"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.questions[0].durationSeconds").value(30))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("answerSpec", "explanation");
    }

    @Test
    @WithMockUser(roles = "SALES_STAFF")
    void usersOutsidePracticeCatalogRolesCannotReadASpeakingTask() throws Exception {
        mockMvc.perform(get("/api/content/practice-tests/{v}/sections/{s}/speaking-task", UUID.randomUUID(),
                UUID.randomUUID())).andExpect(status().isForbidden());

        verifyNoInteractions(speakingTask);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void rejectsNullPackageIdInsideOnlyKeyBeforeCallingSearchUseCase() throws Exception {
        mockMvc.perform(post("/api/content/practice-tests/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"skill":"READING","scope":"PART","only":[{"packageId":null}]}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(search, sources);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void invalidPageSizeIsAClientErrorBeforeSearching() throws Exception {
        mockMvc.perform(post("/api/content/practice-tests/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skill\":\"READING\",\"scope\":\"PART\",\"size\":0}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(search, sources);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void sourcesRouteReturnsTheAvailableSourceCodesAndNames() throws Exception {
        when(sources.execute()).thenReturn(List.of(
                new ListTestSourcesUseCase.Source("IELTS_SPACE_PRO", "The IELTS Space PRO"),
                new ListTestSourcesUseCase.Source("C10_C20", "C10-C20")));

        mockMvc.perform(get("/api/content/practice-tests/sources"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].code")
                        .value("IELTS_SPACE_PRO"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].name")
                        .value("The IELTS Space PRO"));
    }

    @Test
    @WithMockUser(roles = "SALES_STAFF")
    void usersOutsidePracticeCatalogRolesCannotReadSources() throws Exception {
        mockMvc.perform(get("/api/content/practice-tests/sources")).andExpect(status().isForbidden());
        verifyNoInteractions(sources);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void formatsRouteListsTheQuestionFormatsOfOneSkill() throws Exception {
        when(formats.execute(com.ieltspath.content.domain.vo.Skill.WRITING)).thenReturn(List.of(
                new PracticeTestCatalogReader.FormatCount("LINE_GRAPH", 2),
                new PracticeTestCatalogReader.FormatCount("OPINION_ESSAY", 1)));

        mockMvc.perform(get("/api/content/practice-tests/formats").param("skill", "WRITING"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].code")
                        .value("LINE_GRAPH"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].sectionCount")
                        .value(2));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void formatsRouteRejectsAnUnknownSkill() throws Exception {
        mockMvc.perform(get("/api/content/practice-tests/formats").param("skill", "DANCING"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(formats);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void searchRejectsAMalformedFormatCode() throws Exception {
        mockMvc.perform(post("/api/content/practice-tests/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skill\":\"READING\",\"scope\":\"PART\",\"formats\":[\"bad code; drop\"]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(search);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void catalogResponsePreservesSvgCoverAndDoesNotExposeAnswersOrTranscript() throws Exception {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        var card = new PracticeTestCardResult(packageId, "PT-SVG", versionId, "PART", "Reading",
                "READING", null, "IELTS_SPACE_PRO", "The IELTS Space PRO", List.of("GAP_FILLING"),
                1, 600, null, List.of(new PracticeTestCardResult.Part(sectionId, 1, "Passage 1", 1,
                List.of("GAP_FILLING"))));
        String svg = "data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=";
        when(search.execute(any())).thenReturn(new PracticeTestCatalogReader.Page(1, List.of(card)));
        when(search.coverUrl(card)).thenReturn(svg);

        var response = mockMvc.perform(post("/api/content/practice-tests/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skill\":\"READING\",\"scope\":\"PART\"}"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.items[0].coverUrl")
                        .value(svg))
                .andReturn().getResponse().getContentAsString();

        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
        assertThat(json.toString()).doesNotContain("answerSpec", "explanation", "transcript");
    }
}
