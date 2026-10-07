package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.library.application.result.FlashcardResult;
import com.ieltspath.library.application.result.SavedFlashcard;
import com.ieltspath.library.application.usecase.CreateFlashcardUseCase;
import com.ieltspath.library.application.usecase.DeleteFlashcardUseCase;
import com.ieltspath.library.application.usecase.GetFlashcardUseCase;
import com.ieltspath.library.application.usecase.ListFlashcardsUseCase;
import com.ieltspath.library.application.usecase.SavePracticeQuestionFlashcardUseCase;
import com.ieltspath.library.application.usecase.UpdateFlashcardUseCase;
import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
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
import org.springframework.http.MediaType;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class FlashcardPracticeQuestionSecurityTest {
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

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
    }

    private static String body(String sourceType, UUID reference, String extra) {
        return "{\"sourceType\":\"" + sourceType + "\""
                + (reference == null ? "" : ",\"sourceReferenceId\":\"" + reference + "\"")
                + extra + ",\"front\":\"Front\",\"back\":\"Back\"}";
    }

    private static FlashcardResult card(UUID userId, UUID questionId) {
        return FlashcardResult.from(Flashcard.create(
                userId, FlashcardSourceType.PRACTICE_QUESTION, null, questionId, null, "Front", "Back"));
    }

    @Test
    void firstSaveIsCreatedAndASecondReturnsTheSameCardWithOk() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        FlashcardResult saved = card(userId, questionId);
        when(savePracticeQuestionFlashcardUseCase.execute(userId, questionId, "Front", "Back"))
                .thenReturn(new SavedFlashcard(saved, true), new SavedFlashcard(saved, false));
        String token = "Bearer " + signedToken(userId);

        mockMvc.perform(post("/api/learning-support/flashcards").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body("PRACTICE_QUESTION", questionId, "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.id().toString()))
                .andExpect(jsonPath("$.sourceType").value("PRACTICE_QUESTION"))
                .andExpect(jsonPath("$.sourceReferenceId").value(questionId.toString()));
        mockMvc.perform(post("/api/learning-support/flashcards").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body("PRACTICE_QUESTION", questionId, "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.id().toString()));
        verifyNoInteractions(createFlashcardUseCase);
    }

    @Test
    void practiceQuestionCardsWithoutAReferenceOrWithOtherSourcesAreRejected() throws Exception {
        String token = "Bearer " + signedToken(UUID.randomUUID());
        for (String json : List.of(
                body("PRACTICE_QUESTION", null, ""),
                body("PRACTICE_QUESTION", UUID.randomUUID(), ",\"vocabularySenseId\":\"" + UUID.randomUUID() + "\""),
                body("PRACTICE_QUESTION", UUID.randomUUID(), ",\"highlightedText\":\"text\""),
                body("UNKNOWN_SOURCE", UUID.randomUUID(), ""))) {
            mockMvc.perform(post("/api/learning-support/flashcards").header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest());
        }
        verify(savePracticeQuestionFlashcardUseCase, never()).execute(any(), any(), any(), any());
    }

    @Test
    void otherSourcesStillGoThroughTheExistingCreateWith201() throws Exception {
        UUID userId = UUID.randomUUID();
        when(createFlashcardUseCase.execute(userId, FlashcardSourceType.MANUAL, null, null, null, "Front", "Back"))
                .thenReturn(FlashcardResult.from(Flashcard.create(
                        userId, FlashcardSourceType.MANUAL, null, null, null, "Front", "Back")));

        mockMvc.perform(post("/api/learning-support/flashcards")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + signedToken(userId))
                        .contentType(MediaType.APPLICATION_JSON).content(body("MANUAL", null, "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceType").value("MANUAL"));
        verifyNoInteractions(savePracticeQuestionFlashcardUseCase);
    }

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/learning-support/flashcards")
                        .contentType(MediaType.APPLICATION_JSON).content(body("PRACTICE_QUESTION", UUID.randomUUID(), "")))
                .andExpect(status().isUnauthorized());
    }

    private static String signedToken(UUID subject) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(INTERNAL_ISSUER)
                .subject(subject.toString())
                .expiresAt(now.plusSeconds(300))
                .claim("roles", List.of("CUSTOMER"))
                .build();
        OctetSequenceKey key = new OctetSequenceKey.Builder(
                new SecretKeySpec(Base64.getDecoder().decode(INTERNAL_SECRET), "HmacSHA256"))
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
