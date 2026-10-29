package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.content.application.usecase.CreateContentSectionUseCase;
import com.ieltspath.content.application.usecase.CreateDraftPackageVersionUseCase;
import com.ieltspath.content.application.usecase.DeleteContentSectionUseCase;
import com.ieltspath.content.application.usecase.GetAuthorPackageUseCase;
import com.ieltspath.content.application.usecase.ReorderContentSectionsUseCase;
import com.ieltspath.content.application.usecase.UpdateContentSectionUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ContentPackageAuthoringController.class, properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Import(CommonSecurityAutoConfiguration.class)
class ContentPackageAuthoringControllerTest {
    private static final byte[] INTERNAL_KEY = new byte[32];

    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    private static final String SECTION_BODY = "{\"title\":\"Reading Passage 1\",\"sortOrder\":1}";
    private static final String ORDER_BODY = "{\"sectionIds\":[\"" + UUID.randomUUID() + "\"]}";

    @Autowired MockMvc mockMvc;
    @MockitoBean GetAuthorPackageUseCase getPackage;
    @MockitoBean CreateDraftPackageVersionUseCase createDraft;
    @MockitoBean CreateContentSectionUseCase createSection;
    @MockitoBean UpdateContentSectionUseCase updateSection;
    @MockitoBean DeleteContentSectionUseCase deleteSection;
    @MockitoBean ReorderContentSectionsUseCase reorderSections;
    @MockitoBean com.ieltspath.content.application.usecase.AddSectionQuestionUseCase addSectionQuestion;
    @MockitoBean com.ieltspath.content.application.usecase.RemoveAndReorderSectionQuestionsUseCase removeAndReorderQuestions;
    @MockitoBean com.ieltspath.content.application.usecase.ListAuthorPackagesUseCase listPackages;
    @MockitoBean com.ieltspath.content.application.usecase.CreateContentPackageUseCase createPackage;
    @MockitoBean com.ieltspath.content.application.usecase.UpdateContentPackageUseCase updatePackage;
    @MockitoBean com.ieltspath.content.application.usecase.ArchiveContentPackageUseCase archivePackage;
    @MockitoBean com.ieltspath.content.application.usecase.CopyPackageVersionToDraftUseCase copyToDraft;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerCannotReadOrWriteAuthorPackages() throws Exception {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/content/admin/packages/{id}", packageId))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/content/admin/packages/{id}/versions", packageId)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/content/admin/packages/{p}/versions/{v}/sections", packageId, versionId)
                        .contentType(MediaType.APPLICATION_JSON).content(SECTION_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/content/admin/packages/{p}/versions/{v}/sections/{s}", packageId, versionId, sectionId)
                        .contentType(MediaType.APPLICATION_JSON).content(SECTION_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/content/admin/packages/{p}/versions/{v}/sections/{s}", packageId, versionId, sectionId))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/content/admin/packages/{p}/versions/{v}/sections/order", packageId, versionId)
                        .contentType(MediaType.APPLICATION_JSON).content(ORDER_BODY))
                .andExpect(status().isForbidden());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/content/admin/packages"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/content/admin/packages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"X\",\"title\":\"T\",\"packageType\":\"PRACTICE_SET\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/content/admin/packages/{id}", packageId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"T\",\"accessLevel\":\"FREE\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/content/admin/packages/{id}", packageId))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/content/admin/packages/{p}/versions/{v}/copy", packageId, versionId))
                .andExpect(status().isForbidden());

        verifyNoInteractions(getPackage, createDraft, createSection, updateSection, deleteSection, reorderSections,
                listPackages, createPackage, updatePackage, archivePackage, copyToDraft);
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void authorDeletesAPackageWithNoContent() throws Exception {
        UUID packageId = UUID.randomUUID();

        mockMvc.perform(delete("/api/content/admin/packages/{id}", packageId))
                .andExpect(status().isNoContent());

        verify(archivePackage).execute(packageId);
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void updateRefusesABlankTitleBeforeReachingTheUseCase() throws Exception {
        mockMvc.perform(put("/api/content/admin/packages/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\" \",\"accessLevel\":\"FREE\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(updatePackage);
    }
}
