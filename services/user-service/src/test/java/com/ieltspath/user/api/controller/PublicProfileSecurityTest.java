package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.user.application.result.PublicProfileResult;
import com.ieltspath.user.application.usecase.GetPublicProfilesUseCase;
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
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = PublicProfileController.class, properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
class PublicProfileSecurityTest {
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }
    @Autowired MockMvc mvc;
    @MockBean GetPublicProfilesUseCase useCase;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Test
    void identityHeadersCannotAuthenticateEitherEndpoint() throws Exception {
        mvc.perform(get("/api/users/{id}/public-profile", UUID.randomUUID()).header("X-User-Id", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/users/public-profiles").header("X-User-Id", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userIds\":[]}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(useCase);
    }

    @Test
    void externalIssuerCannotAccessEitherEndpoint() throws Exception {
        String token = token(UUID.randomUUID(), "urn:code-base:auth");
        mvc.perform(get("/api/users/{id}/public-profile", UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/users/public-profiles").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userIds\":[]}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(useCase);
    }

    @Test
    void publicProfileExposesOnlyApprovedFieldsAndUsesVerifiedViewer() throws Exception {
        UUID viewer = UUID.randomUUID(), target = UUID.randomUUID();
        when(useCase.execute(viewer, target)).thenReturn(profile(target));
        mvc.perform(get("/api/users/{id}/public-profile", target)
                        .header(HttpHeaders.AUTHORIZATION, token(viewer, "urn:code-base:api-gateway")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(target.toString()))
                .andExpect(jsonPath("$.displayName").value("Display"))
                .andExpect(jsonPath("$.targetBand").value(7.5)).andExpect(jsonPath("$.streak").doesNotExist())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.email").doesNotExist()).andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.roles").doesNotExist()).andExpect(jsonPath("$.timezone").doesNotExist());
        verify(useCase).execute(viewer, target);
    }

    @Test
    void internalBatchUsesVerifiedViewer() throws Exception {
        UUID viewer = UUID.randomUUID(), target = UUID.randomUUID();
        when(useCase.executeBatch(viewer, List.of(target))).thenReturn(List.of(profile(target)));
        mvc.perform(post("/internal/users/public-profiles")
                        .header(HttpHeaders.AUTHORIZATION, token(viewer, "urn:code-base:api-gateway"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userIds\":[\"" + target + "\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.profiles[0].id").value(target.toString()))
                .andExpect(jsonPath("$.profiles[0].length()").value(5))
                .andExpect(jsonPath("$.profiles[0].streak").doesNotExist());
        verify(useCase).executeBatch(viewer, List.of(target));
    }

    @Test
    void internalMentionSearchUsesVerifiedViewerAndBoundedLimit() throws Exception {
        UUID viewer=UUID.randomUUID(), target=UUID.randomUUID();
        when(useCase.search(viewer,"Lan",8)).thenReturn(List.of(profile(target)));
        mvc.perform(get("/internal/users/public-profiles/search?q=Lan&limit=8")
                        .header(HttpHeaders.AUTHORIZATION, token(viewer,"urn:code-base:api-gateway")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.profiles[0].id").value(target.toString()));
        verify(useCase).search(viewer,"Lan",8);
        when(useCase.search(viewer,"x",8)).thenThrow(new IllegalArgumentException("invalid query"));
        mvc.perform(get("/internal/users/public-profiles/search?q=x&limit=8")
                        .header(HttpHeaders.AUTHORIZATION, token(viewer,"urn:code-base:api-gateway")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidAndOversizedInputsReturnBadRequest() throws Exception {
        String token = token(UUID.randomUUID(), "urn:code-base:api-gateway");
        String oversized = "{\"userIds\":[" + String.join(",", Collections.nCopies(101, "\"" + UUID.randomUUID() + "\"")) + "]}";
        for (String body : List.of("{}", "{\"userIds\":[null]}", "{\"userIds\":[\"bad\"]}", oversized)) {
            mvc.perform(post("/internal/users/public-profiles").header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/users/bad/public-profile").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(useCase);
    }

    private PublicProfileResult profile(UUID id) {
        return new PublicProfileResult(id, "Display", null, "Bio", new BigDecimal("7.5"));
    }

    private String token(UUID viewer, String issuer) {
        var key = new OctetSequenceKey.Builder(KEY).build();
        var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(viewer.toString())
                .expiresAt(Instant.now().plusSeconds(60)).claim("roles", List.of("CUSTOMER")).build();
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
