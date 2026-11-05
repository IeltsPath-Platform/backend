package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.application.usecase.ApproveCourseDraftUseCase;
import com.ieltspath.content.application.usecase.CourseDraftUseCases;
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
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {CourseDraftController.class, AiCourseDraftController.class}, properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Import(CommonSecurityAutoConfiguration.class)
class CourseDraftAuthorizationTest {
    private static final byte[] INTERNAL_KEY = new byte[32];

    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean CourseDraftUseCases drafts;
    @MockitoBean ApproveCourseDraftUseCase approve;
    @MockitoBean com.ieltspath.content.application.usecase.RequestCourseDraftUseCase requestCourse;
    @MockitoBean com.ieltspath.content.application.usecase.FindPracticeSetsForPointsUseCase practiceSets;
    @MockitoBean CurrentUserProvider currentUser;

    @Test
    @WithMockUser(roles = {"CUSTOMER", "EXAMINER", "ADMIN"})
    void onlyAContentAuthorMayUseCourseDrafts() throws Exception {
        UUID id = UUID.randomUUID();
        String base = "/api/content/admin/course-drafts";

        mockMvc.perform(post(base).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(base + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"structure\":{}}")).andExpect(status().isForbidden());
        mockMvc.perform(get(base + "/{id}/analysis", id)).andExpect(status().isForbidden());
        mockMvc.perform(post(base + "/{id}/approve", id)).andExpect(status().isForbidden());
        mockMvc.perform(get(base + "/practice-sets").param("knowledgePointIds", id.toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/content/admin/ai-drafts/courses").contentType(MediaType.APPLICATION_JSON)
                .content("{\"bandLevel\":6.5,\"skills\":[\"READING\"],\"topics\":[\"a\"]}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(drafts, approve, requestCourse, practiceSets);
    }
}
