package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.content.application.usecase.ManageRubricsUseCase;
import com.ieltspath.content.domain.aggregate.Rubric;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {RubricController.class, InternalRubricController.class}, properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Import(CommonSecurityAutoConfiguration.class)
class RubricControllerTest {
    private static final byte[] INTERNAL_KEY = new byte[32];

    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean ManageRubricsUseCase rubrics;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnersCannotReadRubricsOrCreateDrafts() throws Exception {
        mockMvc.perform(get("/api/content/rubrics?task=TASK_1")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/content/admin/rubrics").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(rubrics);
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void examinerReadsOnlyTheActiveRubricOnPublicRoute() throws Exception {
        when(rubrics.active("TASK_1")).thenReturn(List.of(activeRubric()));
        mockMvc.perform(get("/api/content/rubrics?task=TASK_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("IELTS_WRITING_TASK_1"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].criteria[0].bandDescriptors[0].band").value(6.5));
        mockMvc.perform(get("/api/content/admin/rubrics?task=TASK_1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void authorCanCreateDraftAndInternalReadersGetTheSharedProjection() throws Exception {
        when(rubrics.create(any(), any(), anyInt(), any(), any(), any())).thenReturn(draftRubric());
        mockMvc.perform(post("/api/content/admin/rubrics").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        when(rubrics.activeRubric("TASK_1")).thenReturn(activeRubric());
        mockMvc.perform(get("/internal/learning-content/rubrics/active?task=TASK_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("IELTS_WRITING_TASK_1"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.sourceNote").exists())
                .andExpect(jsonPath("$.criteria[0].sortOrder").value(1))
                .andExpect(jsonPath("$.criteria[0].bandDescriptors[0].descriptor").value("Descriptor"))
                .andExpect(jsonPath("$.examples[0].essay").value("Example essay"))
                .andExpect(jsonPath("$.status").doesNotExist());

        when(rubrics.byVersion("IELTS_WRITING_TASK_1", 1)).thenReturn(retiredRubric());
        mockMvc.perform(get("/internal/learning-content/rubrics/IELTS_WRITING_TASK_1/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.task").value("TASK_1"))
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    private static String requestBody() {
        return "{\"code\":\"NEW_TASK_1\",\"task\":\"TASK_1\",\"version\":1,\"sourceNote\":\"source\","
                + "\"criteria\":[{\"code\":\"TA\",\"name\":\"TA\",\"description\":\"desc\",\"sortOrder\":1,"
                + "\"bandDescriptors\":[{\"band\":6.5,\"descriptor\":\"Descriptor\"}]}],\"examples\":[]}";
    }

    private static Rubric activeRubric() {
        return new Rubric("IELTS_WRITING_TASK_1", "TASK_1", 1, "Source note", "ACTIVE",
                List.of(new Rubric.Criterion("TA", "Task Achievement", "Description", 1,
                        List.of(new Rubric.BandDescriptor(6.5, "Descriptor")))),
                List.of(new Rubric.Example(6.5, "Prompt", "Example essay", "Commentary")));
    }

    private static Rubric draftRubric() {
        return new Rubric("NEW_TASK_1", "TASK_1", 1, "source", "DRAFT",
                List.of(new Rubric.Criterion("TA", "TA", "desc", 1,
                        List.of(new Rubric.BandDescriptor(6.5, "Descriptor")))), List.of());
    }

    private static Rubric retiredRubric() {
        return new Rubric("IELTS_WRITING_TASK_1", "TASK_1", 1, "Source note", "RETIRED",
                List.of(new Rubric.Criterion("TA", "Task Achievement", "Description", 1,
                        List.of(new Rubric.BandDescriptor(6.5, "Descriptor")))), List.of());
    }
}
