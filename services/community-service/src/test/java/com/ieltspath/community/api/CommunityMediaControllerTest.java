package com.ieltspath.community.api;

import com.ieltspath.commonsecurity.currentuser.CurrentUser;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.commonsecurity.role.CanonicalRoles;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.community.api.controller.CommunityMediaController;
import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.result.StoredMediaResult;
import com.ieltspath.community.application.usecase.UploadCommunityImageUseCase;
import com.ieltspath.community.domain.exception.CommunityRateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommunityMediaController.class)
@TestPropertySource(properties = {
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "app.auth.internal-jwt-secret=SGs1cjhXY1lBOEo4am8ycnVJQkNFWFJjSGhVZlNEVXFQMTZIMjlQMXN4bz0="
})
class CommunityMediaControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CurrentUserProvider currentUserProvider;

    @MockitoBean
    UploadCommunityImageUseCase uploadImageUseCase;

    private static final String VALID_USER_ID = "00000000-0000-0000-0000-000000000001";

    @Test
    void uploadImageRequiresAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2, 3});
        mvc.perform(multipart("/api/community/media/images").file(file).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadImageReturnsCreatedWithMetadata() throws Exception {
        UUID userId = UUID.fromString(VALID_USER_ID);
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new CurrentUser(userId, Set.of(CanonicalRoles.CUSTOMER)));

        when(uploadImageUseCase.execute(any(CommunityActor.class), eq("image/png"), any(byte[].class)))
                .thenReturn(new StoredMediaResult(
                        "https://storage/public/community/community/post/1/uuid.png",
                        "community/post/" + userId + "/uuid.png",
                        "image/png",
                        1234L
                ));

        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/community/media/images")
                        .file(file)
                        .with(jwt().jwt(j -> j.subject(VALID_USER_ID).claim("role", CanonicalRoles.CUSTOMER)))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("https://storage/public/community/community/post/1/uuid.png"))
                .andExpect(jsonPath("$.key").value("community/post/" + userId + "/uuid.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.size").value(1234));
    }

    @Test
    void uploadImageMapsEmptyFileToBadRequest() throws Exception {
        UUID userId = UUID.fromString(VALID_USER_ID);
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new CurrentUser(userId, Set.of(CanonicalRoles.CUSTOMER)));

        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        mvc.perform(multipart("/api/community/media/images")
                        .file(emptyFile)
                        .with(jwt().jwt(j -> j.subject(VALID_USER_ID).claim("role", CanonicalRoles.CUSTOMER)))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadImageMapsTypeNotAllowedToUnsupportedMediaType() throws Exception {
        UUID userId = UUID.fromString(VALID_USER_ID);
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new CurrentUser(userId, Set.of(CanonicalRoles.CUSTOMER)));

        when(uploadImageUseCase.execute(any(), any(), any()))
                .thenThrow(new UploadRejectedException(UploadRejectedException.Reason.TYPE_NOT_ALLOWED));

        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/community/media/images")
                        .file(file)
                        .with(jwt().jwt(j -> j.subject(VALID_USER_ID).claim("role", CanonicalRoles.CUSTOMER)))
                        .with(csrf()))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void uploadImageMapsTooLargeToPayloadTooLarge() throws Exception {
        UUID userId = UUID.fromString(VALID_USER_ID);
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new CurrentUser(userId, Set.of(CanonicalRoles.CUSTOMER)));

        when(uploadImageUseCase.execute(any(), any(), any()))
                .thenThrow(new UploadRejectedException(UploadRejectedException.Reason.TOO_LARGE));

        MockMultipartFile file = new MockMultipartFile("file", "huge.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/community/media/images")
                        .file(file)
                        .with(jwt().jwt(j -> j.subject(VALID_USER_ID).claim("role", CanonicalRoles.CUSTOMER)))
                        .with(csrf()))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void uploadImageMapsRateLimitToTooManyRequests() throws Exception {
        UUID userId = UUID.fromString(VALID_USER_ID);
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new CurrentUser(userId, Set.of(CanonicalRoles.CUSTOMER)));

        when(uploadImageUseCase.execute(any(), any(), any()))
                .thenThrow(new CommunityRateLimitExceededException("Too many uploads. Please try again later"));

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/community/media/images")
                        .file(file)
                        .with(jwt().jwt(j -> j.subject(VALID_USER_ID).claim("role", CanonicalRoles.CUSTOMER)))
                        .with(csrf()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many uploads. Please try again later"));
    }

    @Test
    void uploadImageMapsStorageUnavailableTo503() throws Exception {
        UUID userId = UUID.fromString(VALID_USER_ID);
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new CurrentUser(userId, Set.of(CanonicalRoles.CUSTOMER)));

        when(uploadImageUseCase.execute(any(), any(), any()))
                .thenThrow(StorageException.withoutObject("put", StorageException.Reason.UNAVAILABLE));

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/community/media/images")
                        .file(file)
                        .with(jwt().jwt(j -> j.subject(VALID_USER_ID).claim("role", CanonicalRoles.CUSTOMER)))
                        .with(csrf()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Storage service is currently unavailable"));
    }

    @Test
    void videoUploadEndpointIsGone() throws Exception {
        mvc.perform(post("/api/community/media/videos/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"video/mp4\",\"size\":1000}")
                        .with(jwt().jwt(j -> j.subject(VALID_USER_ID).claim("role", CanonicalRoles.CUSTOMER)))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
