package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.user.application.query.PageQuery;
import com.ieltspath.user.application.result.LoginHistoryResult;
import com.ieltspath.user.application.result.PageResult;
import com.ieltspath.user.application.usecase.GetLoginHistoryUseCase;
import com.ieltspath.user.domain.exception.UserNotFoundException;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoginHistoryController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
    "app.auth.external-jwt-issuer=urn:code-base:auth",
    "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
    "spring.cloud.config.enabled=false"
})
class LoginHistoryControllerSecurityTest {

    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private static final String INTERNAL_ISSUER = "urn:code-base:api-gateway";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetLoginHistoryUseCase getLoginHistoryUseCase;

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/{id}/login-history", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminRolesAreForbidden() throws Exception {
        UUID targetId = UUID.randomUUID();
        for (String role : List.of("CUSTOMER", "CONTENT_AUTHOR", "EXAMINER", "SALES_STAFF")) {
            String token = signedToken(UUID.randomUUID(), List.of(role));
            mockMvc.perform(get("/api/users/{id}/login-history", targetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void adminReadsPagedHistory() throws Exception {
        UUID targetId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        when(getLoginHistoryUseCase.execute(eq(targetId), eq(new PageQuery(1, 10)))).thenReturn(new PageResult<>(
                List.of(new LoginHistoryResult(attemptId, false, "ACCOUNT_LOCKED", "10.0.0.7", "Mozilla/5.0",
                        LocalDateTime.of(2026, 10, 28, 9, 30))),
                1, 10, 11));
        String token = signedToken(UUID.randomUUID(), List.of("ADMIN"));

        mockMvc.perform(get("/api/users/{id}/login-history?page=1&size=10", targetId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(11))
                .andExpect(jsonPath("$.items[0].id").value(attemptId.toString()))
                .andExpect(jsonPath("$.items[0].success").value(false))
                .andExpect(jsonPath("$.items[0].failureReason").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.items[0].ipAddress").value("10.0.0.7"));
    }

    @Test
    void defaultsToFirstPageOfTwenty() throws Exception {
        UUID targetId = UUID.randomUUID();
        when(getLoginHistoryUseCase.execute(eq(targetId), eq(new PageQuery(0, 20))))
                .thenReturn(new PageResult<>(List.of(), 0, 20, 0));
        String token = signedToken(UUID.randomUUID(), List.of("ADMIN"));

        mockMvc.perform(get("/api/users/{id}/login-history", targetId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        verify(getLoginHistoryUseCase).execute(eq(targetId), eq(new PageQuery(0, 20)));
    }

    @Test
    void unknownUserIsNotFound() throws Exception {
        UUID targetId = UUID.randomUUID();
        when(getLoginHistoryUseCase.execute(eq(targetId), eq(new PageQuery(0, 20))))
                .thenThrow(new UserNotFoundException("Không tìm thấy người dùng: " + targetId));
        String token = signedToken(UUID.randomUUID(), List.of("ADMIN"));

        mockMvc.perform(get("/api/users/{id}/login-history", targetId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private String signedToken(UUID subject, List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(INTERNAL_ISSUER)
                .subject(subject.toString())
                .expiresAt(now.plusSeconds(300))
                .claim("roles", roles)
                .build();
        return jwtEncoder().encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private static JwtEncoder jwtEncoder() {
        OctetSequenceKey key = new OctetSequenceKey.Builder(
                new SecretKeySpec(Base64.getDecoder().decode(INTERNAL_SECRET), "HmacSHA256"))
                .build();
        return new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(
                new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
    }
}
