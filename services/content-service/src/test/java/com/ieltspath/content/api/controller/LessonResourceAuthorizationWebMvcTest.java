package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.content.application.result.LessonResourceResult;
import com.ieltspath.content.application.result.LessonResourcesResult;
import com.ieltspath.content.application.usecase.*;
import com.ieltspath.content.domain.aggregate.LessonResource;
import com.ieltspath.content.domain.vo.LessonResourceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LessonResourceAdminController.class, properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Import(CommonSecurityAutoConfiguration.class)
class LessonResourceAuthorizationWebMvcTest {
    private static final byte[] INTERNAL_KEY = new byte[32];

    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean ListLessonResourcesUseCase listResources;
    @MockitoBean CreateLessonResourceUseCase createResource;
    @MockitoBean UpdateLessonResourceUseCase updateResource;
    @MockitoBean DeleteLessonResourceUseCase deleteResource;
    @MockitoBean UpdateLessonSummaryUseCase updateSummary;

    private final UUID lessonId = UUID.randomUUID();
    private final UUID resourceId = UUID.randomUUID();
    private static final String RESOURCE = "{\"type\":\"SLIDE\",\"title\":\"Slide\",\"url\":\"https://a.com\",\"sortOrder\":0}";
    private static final String UPDATE = "{\"title\":\"Slide\",\"url\":\"https://a.com\",\"sortOrder\":0}";

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerCannotReadOrWriteLessonResourcesThroughTheAuthorRoutes() throws Exception {
        mockMvc.perform(get("/api/content/admin/lessons/{id}/resources", lessonId)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/content/admin/lessons/{id}/resources", lessonId)
                .contentType(MediaType.APPLICATION_JSON).content(RESOURCE)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/content/admin/lessons/{id}/resources/{rid}", lessonId, resourceId)
                .contentType(MediaType.APPLICATION_JSON).content(UPDATE)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/content/admin/lessons/{id}/resources/{rid}", lessonId, resourceId))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/content/admin/lessons/{id}/summary", lessonId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"markdown\":\"x\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(listResources, createResource, updateResource, deleteResource, updateSummary);
    }

    @Test
    void anonymousCannotReachTheAuthorRoutes() throws Exception {
        mockMvc.perform(get("/api/content/admin/lessons/{id}/resources", lessonId)).andExpect(status().isUnauthorized());
        verifyNoInteractions(listResources);
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void contentAuthorCanManageLessonResources() throws Exception {
        var result = LessonResourceResult.from(LessonResource.create(lessonId, LessonResourceType.SLIDE, "Slide",
                "https://a.com", 0));
        when(listResources.execute(lessonId)).thenReturn(new LessonResourcesResult(lessonId, null, List.of(result)));
        when(createResource.execute(any())).thenReturn(result);
        when(updateResource.execute(any())).thenReturn(result);

        mockMvc.perform(get("/api/content/admin/lessons/{id}/resources", lessonId)).andExpect(status().isOk());
        mockMvc.perform(post("/api/content/admin/lessons/{id}/resources", lessonId)
                .contentType(MediaType.APPLICATION_JSON).content(RESOURCE)).andExpect(status().isCreated());
        mockMvc.perform(put("/api/content/admin/lessons/{id}/resources/{rid}", lessonId, resourceId)
                .contentType(MediaType.APPLICATION_JSON).content(UPDATE)).andExpect(status().isOk());
        mockMvc.perform(delete("/api/content/admin/lessons/{id}/resources/{rid}", lessonId, resourceId))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/api/content/admin/lessons/{id}/summary", lessonId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"markdown\":\"## Hi\"}")).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanReadLessonResources() throws Exception {
        when(listResources.execute(lessonId)).thenReturn(new LessonResourcesResult(lessonId, null, List.of()));
        mockMvc.perform(get("/api/content/admin/lessons/{id}/resources", lessonId)).andExpect(status().isOk());
    }
}
