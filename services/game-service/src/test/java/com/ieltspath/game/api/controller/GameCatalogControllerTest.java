package com.ieltspath.game.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.game.application.usecase.GetGameCatalogUseCase;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameCatalogController.class)
@Import(GetGameCatalogUseCase.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
class GameCatalogControllerTest {
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @Autowired
    private MockMvc mvc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Test
    void authenticatedLearnerGetsCatalogInScreenOrder() throws Exception {
        mvc.perform(get("/api/games/catalog").header("Authorization", "Bearer " + token(
                        "urn:code-base:api-gateway", Instant.now().plusSeconds(300))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.games.length()").value(2))
                .andExpect(jsonPath("$.games[0].gameType").value("ROCKET"))
                .andExpect(jsonPath("$.games[0].name").value("Tên lửa"))
                .andExpect(jsonPath("$.games[0].status").value("COMING_SOON"))
                .andExpect(jsonPath("$.games[1].gameType").value("WORDLY"))
                .andExpect(jsonPath("$.games[1].name").value("Wordly"))
                .andExpect(jsonPath("$.games[1].status").value("AVAILABLE"));
    }

    @Test
    void missingTokenAndSpoofedIdentityDoNotGrantAccess() throws Exception {
        mvc.perform(get("/api/games/catalog").header("X-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        mvc.perform(get("/api/games/catalog").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        mvc.perform(get("/api/games/catalog").header("Authorization", "Bearer " + token(
                        "urn:code-base:api-gateway", Instant.now().minusSeconds(120))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void externalIssuerIsNotAcceptedDirectlyByService() throws Exception {
        mvc.perform(get("/api/games/catalog").header("Authorization", "Bearer " + token(
                        "urn:code-base:auth", Instant.now().plusSeconds(300))))
                .andExpect(status().isUnauthorized());
    }

    private String token(String issuer, Instant expiresAt) {
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(UUID.randomUUID().toString())
                .expiresAt(expiresAt).claim("roles", List.of("CUSTOMER")).build();
        var key = new OctetSequenceKey.Builder(KEY).build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
