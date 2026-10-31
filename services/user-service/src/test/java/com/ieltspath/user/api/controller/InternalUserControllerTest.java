package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.user.application.result.UserDisplayNameResult;
import com.ieltspath.user.application.usecase.GetStudyTimezoneUseCase;
import com.ieltspath.user.application.usecase.GetUserDisplayNamesUseCase;
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
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalUserController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "app.auth.external-jwt-issuer=urn:code-base:auth",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "spring.cloud.config.enabled=false"
})
class InternalUserControllerTest {
    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private static final String INTERNAL_ISSUER = "urn:code-base:api-gateway";

    @Autowired MockMvc mockMvc;
    @MockBean GetUserDisplayNamesUseCase displayNames;
    @MockBean GetStudyTimezoneUseCase studyTimezone;

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
    }

    @Test
    void unauthenticatedInternalRequestsAreRejected() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/internal/users/display-names").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[\"" + id + "\"]}")).andExpect(status().isUnauthorized());
    }

    @Test
    void studyTimezoneIsTheOneOnTheProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        when(studyTimezone.execute(userId)).thenReturn("America/Los_Angeles");

        mockMvc.perform(get("/internal/users/{id}/study-timezone", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("America/Los_Angeles"));
        verify(studyTimezone).execute(userId);
    }

    @Test
    void studyTimezoneRequiresAnInternalToken() throws Exception {
        mockMvc.perform(get("/internal/users/{id}/study-timezone", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(studyTimezone);
    }

    @Test
    void displayNameBatchResponseContainsNoEmailAndOmitsUnknownIds() throws Exception {
        UUID existingId = UUID.randomUUID();
        UUID unknownId = UUID.randomUUID();
        when(displayNames.execute(anyList())).thenReturn(List.of(new UserDisplayNameResult(existingId, "Mina")));

        mockMvc.perform(post("/internal/users/display-names")
                        .header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + existingId + "\",\"" + unknownId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(existingId.toString()))
                .andExpect(jsonPath("$.items[0].displayName").value("Mina"))
                .andExpect(jsonPath("$.items[0].email").doesNotExist());
        verify(displayNames).execute(eq(List.of(existingId, unknownId)));
    }

    @Test
    void displayNameBatchRequiresBetweenOneAndFiftyIds() throws Exception {
        UUID principal = UUID.randomUUID();
        String tooMany = "[\"" + java.util.stream.IntStream.range(0, 51).mapToObj(i -> UUID.randomUUID().toString())
                .collect(java.util.stream.Collectors.joining("\",\"")) + "\"]";
        mockMvc.perform(post("/internal/users/display-names").header(HttpHeaders.AUTHORIZATION, bearer(principal))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/internal/users/display-names").header(HttpHeaders.AUTHORIZATION, bearer(principal))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ids\":" + tooMany + "}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(displayNames);
    }

    private String bearer(UUID subject) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(INTERNAL_ISSUER).subject(subject.toString())
                .expiresAt(now.plusSeconds(300)).claim("roles", List.of("CUSTOMER")).build();
        JwtEncoder encoder = new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(
                new ImmutableJWKSet<SecurityContext>(new JWKSet(new OctetSequenceKey.Builder(
                        new SecretKeySpec(Base64.getDecoder().decode(INTERNAL_SECRET), "HmacSHA256")).build())));
        return "Bearer " + encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
