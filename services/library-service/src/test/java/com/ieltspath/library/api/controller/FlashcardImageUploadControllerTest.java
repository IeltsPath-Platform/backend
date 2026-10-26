package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.library.application.result.StoredImageResult;
import com.ieltspath.library.application.usecase.*;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FlashcardController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "app.auth.external-jwt-issuer=urn:code-base:auth",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class FlashcardImageUploadControllerTest {
    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private static final String INTERNAL_ISSUER = "urn:code-base:api-gateway";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateFlashcardUseCase createFlashcardUseCase;
    @MockBean
    private UpdateFlashcardUseCase updateFlashcardUseCase;
    @MockBean
    private GetFlashcardUseCase getFlashcardUseCase;
    @MockBean
    private ListFlashcardsUseCase listFlashcardsUseCase;
    @MockBean
    private DeleteFlashcardUseCase deleteFlashcardUseCase;
    @MockBean
    private SavePracticeQuestionFlashcardUseCase savePracticeQuestionFlashcardUseCase;
    @MockBean
    private GetDueFlashcardsUseCase getDueFlashcardsUseCase;
    @MockBean
    private ReviewFlashcardUseCase reviewFlashcardUseCase;
    @MockBean
    private UploadFlashcardImageUseCase uploadFlashcardImageUseCase;

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
    }

    @Test
    void uploadImageRequiresAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/learning-support/flashcards/images").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadImageReturns201WithKeyAndUrl() throws Exception {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2, 3});
        when(uploadFlashcardImageUseCase.execute(eq(userId), eq("image/png"), any()))
                .thenReturn(new StoredImageResult("images/" + userId + "/uuid.png",
                        "https://storage/images/" + userId + "/uuid.png", "image/png", 3));

        mockMvc.perform(multipart("/api/learning-support/flashcards/images")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + signedInternalToken(userId, "CUSTOMER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("images/" + userId + "/uuid.png"))
                .andExpect(jsonPath("$.url").value("https://storage/images/" + userId + "/uuid.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.size").value(3));
    }

    @Test
    void uploadEmptyFileReturns400() throws Exception {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/api/learning-support/flashcards/images")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + signedInternalToken(userId, "CUSTOMER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadInvalidTypeReturns400() throws Exception {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", new byte[]{1, 2, 3});
        when(uploadFlashcardImageUseCase.execute(eq(userId), eq("text/plain"), any()))
                .thenThrow(new UploadRejectedException(UploadRejectedException.Reason.TYPE_NOT_ALLOWED));

        mockMvc.perform(multipart("/api/learning-support/flashcards/images")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + signedInternalToken(userId, "CUSTOMER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadTooLargeReturns413() throws Exception {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "huge.png", "image/png", new byte[]{1, 2, 3});
        when(uploadFlashcardImageUseCase.execute(eq(userId), eq("image/png"), any()))
                .thenThrow(new UploadRejectedException(UploadRejectedException.Reason.TOO_LARGE));

        mockMvc.perform(multipart("/api/learning-support/flashcards/images")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + signedInternalToken(userId, "CUSTOMER")))
                .andExpect(status().isPayloadTooLarge());
    }

    private static String signedInternalToken(UUID subject, String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(INTERNAL_ISSUER)
                .subject(subject.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("roles", List.of(role))
                .build();
        OctetSequenceKey key = new OctetSequenceKey.Builder(
                new SecretKeySpec(Base64.getDecoder().decode(INTERNAL_SECRET), "HmacSHA256")).build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
