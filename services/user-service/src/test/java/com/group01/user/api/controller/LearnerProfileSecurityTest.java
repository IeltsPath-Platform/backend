package com.group01.user.api.controller;

import com.group01.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.group01.user.application.command.UpdateLearnerProfileCommand;
import com.group01.user.application.result.LearnerProfileResult;
import com.group01.user.application.usecase.GetLearnerProfileUseCase;
import com.group01.user.application.usecase.UpdateLearnerProfileUseCase;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LearnerProfileController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
    "app.auth.external-jwt-issuer=urn:code-base:auth",
    "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
    "spring.cloud.config.enabled=false"
})
class LearnerProfileSecurityTest {

    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private static final String INTERNAL_ISSUER = "urn:code-base:api-gateway";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetLearnerProfileUseCase getLearnerProfileUseCase;
    @MockBean
    private UpdateLearnerProfileUseCase updateLearnerProfileUseCase;

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void learnerCanGetOwnProfileViaMe() throws Exception {
        UUID userId = UUID.randomUUID();
        when(getLearnerProfileUseCase.execute(userId)).thenReturn(profileResult(userId, "John Doe"));

        String token = signedToken(INTERNAL_SECRET, userId, INTERNAL_ISSUER, List.of("CUSTOMER"));

        mockMvc.perform(get("/api/users/me/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("John Doe"));

        verify(getLearnerProfileUseCase).execute(userId);
    }

    @Test
    void learnerCanUpdateOwnProfileViaMe() throws Exception {
        UUID userId = UUID.randomUUID();
        when(updateLearnerProfileUseCase.execute(any(UpdateLearnerProfileCommand.class)))
                .thenReturn(profileResult(userId, "New Name"));

        String token = signedToken(INTERNAL_SECRET, userId, INTERNAL_ISSUER, List.of("CUSTOMER"));

        String body = """
                {
                    "displayName": "New Name",
                    "bio": "IELTS learner",
                    "selfReportedBand": 6.5,
                    "timezone": "Asia/Ho_Chi_Minh",
                    "visibility": "PUBLIC"
                }
                """;

        mockMvc.perform(put("/api/users/me/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("New Name"));

        verify(updateLearnerProfileUseCase).execute(any(UpdateLearnerProfileCommand.class));
    }

    private LearnerProfileResult profileResult(UUID userId, String displayName) {
        return new LearnerProfileResult(
                userId,
                displayName,
                null,
                "Bio",
                BigDecimal.valueOf(7.0),
                "Asia/Ho_Chi_Minh",
                "PUBLIC",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private String signedToken(String secret, UUID subject, String issuer, List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject.toString())
                .expiresAt(now.plusSeconds(300))
                .claim("roles", roles)
                .build();
        return jwtEncoder(secret).encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private static JwtEncoder jwtEncoder(String secret) {
        OctetSequenceKey key = new OctetSequenceKey.Builder(
                new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256"))
                .build();
        return new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(
                new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
    }
}
