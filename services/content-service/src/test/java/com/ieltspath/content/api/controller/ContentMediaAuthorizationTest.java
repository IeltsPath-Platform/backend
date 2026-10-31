package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.application.usecase.SectionMediaUseCases;
import com.ieltspath.content.application.usecase.UploadContentAudioUseCase;
import com.ieltspath.content.application.usecase.UploadContentImageUseCase;
import com.ieltspath.content.application.result.StoredMediaResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ContentMediaController.class, SectionMediaController.class}, properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Import(CommonSecurityAutoConfiguration.class)
class ContentMediaAuthorizationTest {
    private static final byte[] INTERNAL_KEY = new byte[32];

    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean UploadContentAudioUseCase uploadAudio;
    @MockitoBean UploadContentImageUseCase uploadImage;
    @MockitoBean SectionMediaUseCases media;
    @MockitoBean CurrentUserProvider currentUser;

    @Test
    @WithMockUser(roles = {"CUSTOMER", "EXAMINER"})
    void onlyAContentAuthorMayUploadAudioOrChangeSectionMedia() throws Exception {
        String section = "/api/content/admin/packages/{p}/versions/{v}/sections/{s}";
        UUID p = UUID.randomUUID();
        UUID v = UUID.randomUUID();
        UUID s = UUID.randomUUID();

        mockMvc.perform(multipart("/api/content/admin/media/audio")
                        .file(new MockMultipartFile("file", "a.mp3", "audio/mpeg", new byte[]{1})))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/content/admin/media/image")
                        .file(new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1})))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(section + "/passage", p, v, s).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"x\"}")).andExpect(status().isForbidden());
        mockMvc.perform(delete(section + "/passage", p, v, s)).andExpect(status().isForbidden());
        mockMvc.perform(put(section + "/audio", p, v, s).contentType(MediaType.APPLICATION_JSON)
                .content("{\"mediaReference\":\"a/b.mp3\",\"durationSeconds\":5}")).andExpect(status().isForbidden());
        mockMvc.perform(delete(section + "/audio", p, v, s)).andExpect(status().isForbidden());

        verifyNoInteractions(uploadAudio, uploadImage, media);
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void contentAuthorCanRequestPremiumAudioUpload() throws Exception {
        UUID authorId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(authorId);
        when(uploadAudio.execute(org.mockito.ArgumentMatchers.eq(authorId),
                org.mockito.ArgumentMatchers.eq("audio/mpeg"), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(com.ieltspath.content.domain.vo.AccessTier.PREMIUM)))
                .thenReturn(new StoredMediaResult("content-audio/a/f.mp3", null, "audio/mpeg", 1));

        mockMvc.perform(multipart("/api/content/admin/media/audio")
                        .file(new MockMultipartFile("file", "a.mp3", "audio/mpeg", new byte[]{1}))
                        .param("tier", "PREMIUM"))
                .andExpect(status().isCreated());

        verify(uploadAudio).execute(org.mockito.ArgumentMatchers.eq(authorId),
                org.mockito.ArgumentMatchers.eq("audio/mpeg"), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(com.ieltspath.content.domain.vo.AccessTier.PREMIUM));
    }
}
