package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.usecase.GetSpeakingRecordingUrlUseCase;
import com.ieltspath.assessment.application.usecase.UploadSpeakingRecordingUseCase;
import com.ieltspath.assessment.domain.exception.AssessmentAccessDeniedException;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SpeakingRecordingController.class, properties = "spring.cloud.config.enabled=false")
@Import(SpeakingRecordingControllerTest.MethodSecurityConfig.class)
class SpeakingRecordingControllerTest {
    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                    .exceptionHandling(errors -> errors.authenticationEntryPoint(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                    .build();
        }
    }

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

    @Test
    void uploadRequiresAuthentication() throws Exception {
        mvc.perform(multipart("/api/assessments/speaking-recordings")
                        .file(validFile()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(upload);
    }

    @Test
    void uploadRejectsAnAuthenticatedUserWithoutTheLearnerRole() throws Exception {
        mvc.perform(multipart("/api/assessments/speaking-recordings")
                        .file(validFile()).with(user("examiner").roles("EXAMINER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(upload);
    }

    @Test
    void recordingUrlRejectsAUserWhoDoesNotOwnTheSubmission() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(userId);
        when(recordingUrl.execute(submissionId, userId))
                .thenThrow(new AssessmentAccessDeniedException("not the owner"));

        mvc.perform(get("/api/assessments/submissions/{id}/recording-url", submissionId)
                        .with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadValidationErrorsReturnBadRequest() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.webm", "audio/webm", new byte[0]);
        MockMultipartFile svg = new MockMultipartFile("file", "recording.svg", "image/svg+xml", "<svg/>".getBytes());
        MockMultipartFile fakeAudio = new MockMultipartFile("file", "fake.webm", "audio/webm", "not audio".getBytes());
        doThrow(new UploadRejectedException(UploadRejectedException.Reason.EMPTY))
                .when(upload).execute(any(), any(), any());
        mvc.perform(multipart("/api/assessments/speaking-recordings").file(empty)
                        .with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isBadRequest());
        doThrow(new UploadRejectedException(UploadRejectedException.Reason.TYPE_NOT_ALLOWED))
                .when(upload).execute(any(), any(), any());
        mvc.perform(multipart("/api/assessments/speaking-recordings").file(svg)
                        .with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isBadRequest());
        doThrow(new UploadRejectedException(UploadRejectedException.Reason.CONTENT_MISMATCH))
                .when(upload).execute(any(), any(), any());
        mvc.perform(multipart("/api/assessments/speaking-recordings").file(fakeAudio)
                        .with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversizedUploadReturnsPayloadTooLarge() throws Exception {
        doThrow(new MaxUploadSizeExceededException(5L * 1024 * 1024))
                .when(upload).execute(any(), any(), any());

        mvc.perform(multipart("/api/assessments/speaking-recordings").file(validFile())
                        .with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void storageFailureReturnsServiceUnavailableWithoutExposingStorageDetails() throws Exception {
        doThrow(StorageException.withoutObject("put", StorageException.Reason.UPSTREAM_ERROR))
                .when(upload).execute(any(), any(), any());

        mvc.perform(multipart("/api/assessments/speaking-recordings").file(validFile())
                        .with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Recording storage is unavailable"))
                .andExpect(result -> assertFalse(result.getResponse().getContentAsString().contains("UPSTREAM_ERROR")));
    }

    private static MockMultipartFile validFile() {
        return new MockMultipartFile("file", "answer.webm", "audio/webm",
                new byte[]{0x1a, 0x45, (byte) 0xdf, (byte) 0xa3});
    }
}
