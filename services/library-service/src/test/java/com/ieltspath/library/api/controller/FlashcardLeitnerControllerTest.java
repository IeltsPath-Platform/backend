package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.library.application.query.DueFlashcardsQuery;
import com.ieltspath.library.application.result.FlashcardResult;
import com.ieltspath.library.application.result.PageResult;
import com.ieltspath.library.application.usecase.CreateFlashcardUseCase;
import com.ieltspath.library.application.usecase.DeleteFlashcardUseCase;
import com.ieltspath.library.application.usecase.GetDueFlashcardsUseCase;
import com.ieltspath.library.application.usecase.GetFlashcardUseCase;
import com.ieltspath.library.application.usecase.ListFlashcardsUseCase;
import com.ieltspath.library.application.usecase.ReviewFlashcardUseCase;
import com.ieltspath.library.application.usecase.SavePracticeQuestionFlashcardUseCase;
import com.ieltspath.library.application.usecase.UpdateFlashcardUseCase;
import com.ieltspath.library.application.usecase.UploadFlashcardImageUseCase;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class FlashcardLeitnerControllerTest {
    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

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
    void dueCardsAreOwnerScopedAndIncludeLeitnerAndVocabularyFields() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Flashcard card = Flashcard.create(userId, FlashcardSourceType.MANUAL,
                null, null, null, "Front", "Back");
        when(getDueFlashcardsUseCase.execute(eq(userId), eq(new DueFlashcardsQuery(deckId, 1, 10))))
                .thenReturn(new PageResult<>(List.of(FlashcardResult.from(card)), 1, 10, 21));

        mockMvc.perform(get("/api/learning-support/flashcards/due")
                        .queryParam("deckId", deckId.toString()).queryParam("page", "1").queryParam("limit", "10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + signedToken(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.total").value(21))
                .andExpect(jsonPath("$.items[0].leitnerBox").value(1))
                .andExpect(jsonPath("$.items[0].nextReviewAt").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.items[0].ipa").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.items[0].pronunciationAudioReference").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.items[0].exampleSentence").value(org.hamcrest.Matchers.nullValue()));

        verify(getDueFlashcardsUseCase).execute(eq(userId), eq(new DueFlashcardsQuery(deckId, 1, 10)));
    }

    @Test
    void dueCardsRejectLimitsAboveFifty() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/api/learning-support/flashcards/due").queryParam("limit", "51")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + signedToken(userId)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(getDueFlashcardsUseCase);
    }

    @Test
    void dueCardsRequireAnAuthenticatedLearner() throws Exception {
        mockMvc.perform(get("/api/learning-support/flashcards/due")).andExpect(status().isUnauthorized());
        verifyNoInteractions(getDueFlashcardsUseCase);
    }

    @Test
    void reviewReturnsUpdatedScheduleAndRequiresBothRequestFields() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID flashcardId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        Flashcard card = Flashcard.create(userId, FlashcardSourceType.MANUAL,
                null, null, null, "Front", "Back");
        card.review(true, requestId, Instant.parse("2026-10-17T00:00:00Z"), List.of(1, 2, 4, 8, 16));
        when(reviewFlashcardUseCase.execute(userId, flashcardId, true, requestId))
                .thenReturn(FlashcardResult.from(card));
        String token = "Bearer " + signedToken(userId);

        mockMvc.perform(post("/api/learning-support/flashcards/{id}/reviews", flashcardId)
                        .header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correct\":true,\"requestId\":\"" + requestId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leitnerBox").value(2))
                .andExpect(jsonPath("$.nextReviewAt").value("2026-10-19T00:00:00Z"));

        mockMvc.perform(post("/api/learning-support/flashcards/{id}/reviews", flashcardId)
                        .header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correct\":true}"))
                .andExpect(status().isBadRequest());

        verify(reviewFlashcardUseCase).execute(userId, flashcardId, true, requestId);
        verify(reviewFlashcardUseCase, never()).execute(eq(userId), eq(flashcardId), eq(true), eq(null));
    }

    private static String signedToken(UUID subject) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("urn:code-base:api-gateway")
                .subject(subject.toString()).expiresAt(now.plusSeconds(300)).claim("roles", List.of("CUSTOMER")).build();
        OctetSequenceKey key = new OctetSequenceKey.Builder(
                new SecretKeySpec(Base64.getDecoder().decode(INTERNAL_SECRET), "HmacSHA256")).build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
        return encoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
