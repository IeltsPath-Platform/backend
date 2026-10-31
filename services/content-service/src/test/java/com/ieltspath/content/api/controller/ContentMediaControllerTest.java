package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.result.StoredMediaResult;
import com.ieltspath.content.application.usecase.SectionMediaUseCases;
import com.ieltspath.content.application.usecase.UploadContentAudioUseCase;
import com.ieltspath.content.domain.exception.VersionNotDraftException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Upload and section media routes: response shapes and error mapping. Security is covered separately. */
class ContentMediaControllerTest {

    private final UploadContentAudioUseCase upload = mock(UploadContentAudioUseCase.class);
    private final SectionMediaUseCases media = mock(SectionMediaUseCases.class);
    private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    private final UUID userId = UUID.randomUUID();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(currentUser.requireUserId()).thenReturn(userId);
        mockMvc = MockMvcBuilders.standaloneSetup(new ContentMediaController(upload, currentUser),
                        new SectionMediaController(media))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private static MockMultipartFile file() {
        return new MockMultipartFile("file", "a.mp3", "audio/mpeg", new byte[]{'I', 'D', '3', 0});
    }

    @Test
    void uploadReturnsTheKeyAndUrl() throws Exception {
        when(upload.execute(eq(userId), eq("audio/mpeg"), any())).thenReturn(
                new StoredMediaResult("content-audio/u/f.mp3", "https://media.example.test/content-audio/u/f.mp3",
                        "audio/mpeg", 4));

        mockMvc.perform(multipart("/api/content/admin/media/audio").file(file()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mediaReference").value("content-audio/u/f.mp3"))
                .andExpect(jsonPath("$.mediaUrl").value("https://media.example.test/content-audio/u/f.mp3"))
                .andExpect(jsonPath("$.contentType").value("audio/mpeg"))
                .andExpect(jsonPath("$.sizeBytes").value(4));
    }

    @Test
    void emptyFileIsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/content/admin/media/audio")
                        .file(new MockMultipartFile("file", "a.mp3", "audio/mpeg", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.code").value("EMPTY_FILE"));
    }

    @Test
    void wrongFormatIsBadRequestWithItsCode() throws Exception {
        when(upload.execute(any(), any(), any()))
                .thenThrow(new UploadRejectedException(UploadRejectedException.Reason.CONTENT_MISMATCH));

        mockMvc.perform(multipart("/api/content/admin/media/audio").file(file()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.code").value("UNSUPPORTED_AUDIO"));
    }

    @Test
    void oversizeFileIsPayloadTooLarge() throws Exception {
        when(upload.execute(any(), any(), any()))
                .thenThrow(new UploadRejectedException(UploadRejectedException.Reason.TOO_LARGE));

        mockMvc.perform(multipart("/api/content/admin/media/audio").file(file()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.details.code").value("AUDIO_TOO_LARGE"));
    }

    @Test
    void storageFailureIsServiceUnavailableWithoutLeakingDetails() throws Exception {
        when(upload.execute(any(), any(), any())).thenThrow(
                StorageException.withoutObject("put", StorageException.Reason.MISCONFIGURED));

        mockMvc.perform(multipart("/api/content/admin/media/audio").file(file()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.details.code").value("STORAGE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("File storage is unavailable"));
    }

    @Test
    void passageIsReturnedAfterItIsSet() throws Exception {
        UUID p = UUID.randomUUID();
        UUID v = UUID.randomUUID();
        UUID s = UUID.randomUUID();
        when(media.setPassage(p, v, s, "Hello")).thenReturn("Hello");

        mockMvc.perform(put("/api/content/admin/packages/{p}/versions/{v}/sections/{s}/passage", p, v, s)
                        .contentType("application/json").content("{\"text\":\"Hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passage").value("Hello"));
    }

    @Test
    void blankPassageIsRejectedByValidation() throws Exception {
        mockMvc.perform(put("/api/content/admin/packages/{p}/versions/{v}/sections/{s}/passage",
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
                        .contentType("application/json").content("{\"text\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void audioRequestNeedsAReferenceAndADuration() throws Exception {
        mockMvc.perform(put("/api/content/admin/packages/{p}/versions/{v}/sections/{s}/audio",
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
                        .contentType("application/json").content("{\"mediaReference\":\"a/b.mp3\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clearingOnAPublishedVersionIsBadRequestWithItsCode() throws Exception {
        UUID p = UUID.randomUUID();
        UUID v = UUID.randomUUID();
        UUID s = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new VersionNotDraftException()).when(media).clearAudio(p, v, s);

        mockMvc.perform(delete("/api/content/admin/packages/{p}/versions/{v}/sections/{s}/audio", p, v, s))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.code").value("VERSION_NOT_DRAFT"));
    }
}
