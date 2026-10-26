package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.application.usecase.CourseImportUseCases;
import com.ieltspath.content.application.usecase.DownloadImportTemplateUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CourseImportController.class, properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Import(CommonSecurityAutoConfiguration.class)
class CourseImportAuthorizationTest {
    private static final byte[] INTERNAL_KEY = new byte[32];

    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean DownloadImportTemplateUseCase template;
    @MockitoBean CourseImportUseCases imports;
    @MockitoBean CurrentUserProvider currentUser;

    @Test
    @WithMockUser(roles = {"CUSTOMER", "EXAMINER", "ADMIN"})
    void onlyAContentAuthorMayUseTheImport() throws Exception {
        String base = "/api/content/admin/course-import";
        MockMultipartFile file = new MockMultipartFile("file", "course.xlsx", "application/octet-stream",
                new byte[]{'P', 'K', 3, 4});

        mockMvc.perform(get(base + "/template")).andExpect(status().isForbidden());
        mockMvc.perform(multipart(base + "/validate").file(file)).andExpect(status().isForbidden());
        mockMvc.perform(multipart(base).file(file)).andExpect(status().isForbidden());

        verifyNoInteractions(template, imports);
    }

    @Test
    void anonymousCallersAreRefused() throws Exception {
        mockMvc.perform(get("/api/content/admin/course-import/template")).andExpect(status().isUnauthorized());

        verifyNoInteractions(template, imports);
    }
}
