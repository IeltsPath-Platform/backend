package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.usecase.GetSpeakingRecordingUrlUseCase;
import com.ieltspath.assessment.application.usecase.UploadSpeakingRecordingUseCase;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SpeakingRecordingController.class, properties = "spring.cloud.config.enabled=false")
@Import(GradingControllerTest.MethodSecurityConfig.class)
class SpeakingRecordingControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean UploadSpeakingRecordingUseCase upload;
    @MockitoBean GetSpeakingRecordingUrlUseCase recordingUrl;

    @Test
    void uploadReturnsCreatedWithObjectReferenceAndNoUrl() throws Exception {
        UUID userId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(userId);
        when(upload.execute(eq(userId), eq("audio/webm"), any(byte[].class)))
                .thenReturn(new UploadSpeakingRecordingUseCase.RecordingResult(
                        "speaking/" + userId + "/recording.webm", "audio/webm", 12));
        MockMultipartFile file = new MockMultipartFile("file", "answer.webm", "audio/webm",
                new byte[]{0x1a, 0x45, (byte) 0xdf, (byte) 0xa3});

        mvc.perform(multipart("/api/assessments/speaking-recordings").file(file)
                        .with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mediaReference").value("speaking/" + userId + "/recording.webm"))
                .andExpect(jsonPath("$.contentType").value("audio/webm"))
                .andExpect(jsonPath("$.sizeBytes").value(12))
                .andExpect(jsonPath("$.url").doesNotExist());
        verify(upload).execute(eq(userId), eq("audio/webm"), any(byte[].class));
    }
}
