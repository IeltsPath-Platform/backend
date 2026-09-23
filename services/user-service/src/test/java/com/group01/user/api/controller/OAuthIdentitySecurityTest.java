package com.group01.user.api.controller;

import com.group01.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.group01.user.application.result.OAuthIdentityResult;
import com.group01.user.application.usecase.GetOAuthIdentitiesUseCase;
import com.group01.user.application.usecase.UnlinkOAuthIdentityUseCase;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OAuthIdentityController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
    "app.auth.external-jwt-issuer=urn:code-base:auth",
    "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
    "spring.cloud.config.enabled=false"
})
class OAuthIdentitySecurityTest {

    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private static final String INTERNAL_ISSUER = "urn:code-base:api-gateway";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetOAuthIdentitiesUseCase getOAuthIdentitiesUseCase;
    @MockBean
    private UnlinkOAuthIdentityUseCase unlinkOAuthIdentityUseCase;

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me/oauth"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void learnerCanGetOwnOAuthIdentities() throws Exception {
        UUID userId = UUID.randomUUID();
        when(getOAuthIdentitiesUseCase.execute(userId)).thenReturn(List.of(
                new OAuthIdentityResult(UUID.randomUUID(), userId, "GOOGLE", "sub-123", LocalDateTime.now(), LocalDateTime.now())
        ));

        String token = signedToken(INTERNAL_SECRET, userId, INTERNAL_ISSUER, List.of("CUSTOMER"));

        mockMvc.perform(get("/api/users/me/oauth")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("GOOGLE"));

        verify(getOAuthIdentitiesUseCase).execute(userId);
    }

    @Test
    void learnerCanUnlinkOwnOAuthIdentity() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = signedToken(INTERNAL_SECRET, userId, INTERNAL_ISSUER, List.of("CUSTOMER"));

        mockMvc.perform(delete("/api/users/me/oauth/{provider}", "GOOGLE")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hủy liên kết tài khoản OAuth thành công"));

        verify(unlinkOAuthIdentityUseCase).execute(userId, "GOOGLE");
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
