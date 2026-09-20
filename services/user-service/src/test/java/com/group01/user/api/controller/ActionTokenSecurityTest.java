package com.group01.user.api.controller;

import com.group01.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.group01.user.application.command.ForgotPasswordCommand;
import com.group01.user.application.command.ResetPasswordCommand;
import com.group01.user.application.result.AccountActionTokenResult;
import com.group01.user.application.usecase.ForgotPasswordUseCase;
import com.group01.user.application.usecase.GetActionTokensByUserIdUseCase;
import com.group01.user.application.usecase.GetUserByIdUseCase;
import com.group01.user.application.usecase.LoginUseCase;
import com.group01.user.application.usecase.LogoutUseCase;
import com.group01.user.application.usecase.RefreshTokenUseCase;
import com.group01.user.application.usecase.ResetPasswordUseCase;
import com.group01.user.application.usecase.RevokeUserActionTokensUseCase;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, UserAdminActionTokenController.class})
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "app.auth.external-jwt-issuer=urn:code-base:auth",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "app.security.public-endpoints[0].method=POST",
        "app.security.public-endpoints[0].patterns[0]=/auth/login",
        "app.security.public-endpoints[0].patterns[1]=/auth/refresh",
        "app.security.public-endpoints[0].patterns[2]=/auth/logout",
        "app.security.public-endpoints[0].patterns[3]=/auth/forgot-password",
        "app.security.public-endpoints[0].patterns[4]=/auth/reset-password",
        "spring.cloud.config.enabled=false"
})
class ActionTokenSecurityTest {
    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private static final String INTERNAL_ISSUER = "urn:code-base:api-gateway";

    @Autowired
    private MockMvc mockMvc;

    @MockBean private LoginUseCase loginUseCase;
    @MockBean private RefreshTokenUseCase refreshTokenUseCase;
    @MockBean private LogoutUseCase logoutUseCase;
    @MockBean private ForgotPasswordUseCase forgotPasswordUseCase;
    @MockBean private ResetPasswordUseCase resetPasswordUseCase;
    @MockBean private GetUserByIdUseCase getUserByIdUseCase;

    @MockBean private GetActionTokensByUserIdUseCase getActionTokensByUserIdUseCase;
    @MockBean private RevokeUserActionTokensUseCase revokeUserActionTokensUseCase;

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
    }

    @Test
    void forgotPasswordIsPublic() throws Exception {
        when(forgotPasswordUseCase.execute(any(ForgotPasswordCommand.class))).thenReturn("raw-token");

        String body = """
                {
                    "email": "test@example.com"
                }
                """;

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mã xác nhận đặt lại mật khẩu đã được xử lý"));

        verify(forgotPasswordUseCase).execute(any(ForgotPasswordCommand.class));
    }

    @Test
    void resetPasswordIsPublic() throws Exception {
        String body = """
                {
                    "token": "valid-token",
                    "newPassword": "newpassword123"
                }
                """;

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đặt lại mật khẩu thành công"));

        verify(resetPasswordUseCase).execute(any(ResetPasswordCommand.class));
    }

    @Test
    void adminCanGetActionTokensById() throws Exception {
        UUID userId = UUID.randomUUID();
        when(getActionTokensByUserIdUseCase.execute(userId)).thenReturn(List.of(
                new AccountActionTokenResult(UUID.randomUUID(), userId, "PASSWORD_RESET",
                        LocalDateTime.now().plusMinutes(15), null, LocalDateTime.now())
        ));

        String adminToken = signedToken(INTERNAL_SECRET, UUID.randomUUID(), INTERNAL_ISSUER, List.of("ADMIN"));

        mockMvc.perform(get("/api/users/{id}/action-tokens", userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].purpose").value("PASSWORD_RESET"));

        verify(getActionTokensByUserIdUseCase).execute(userId);
    }

    @Test
    void learnerCannotGetActionTokensById() throws Exception {
        UUID userId = UUID.randomUUID();
        String learnerToken = signedToken(INTERNAL_SECRET, userId, INTERNAL_ISSUER, List.of("CUSTOMER"));

        mockMvc.perform(get("/api/users/{id}/action-tokens", userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanRevokeActionTokensById() throws Exception {
        UUID userId = UUID.randomUUID();
        String adminToken = signedToken(INTERNAL_SECRET, UUID.randomUUID(), INTERNAL_ISSUER, List.of("ADMIN"));

        mockMvc.perform(delete("/api/users/{id}/action-tokens", userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Thu hồi toàn bộ token của người dùng thành công"));

        verify(revokeUserActionTokensUseCase).execute(userId);
    }

    @Test
    void learnerCannotRevokeActionTokensById() throws Exception {
        UUID userId = UUID.randomUUID();
        String learnerToken = signedToken(INTERNAL_SECRET, userId, INTERNAL_ISSUER, List.of("CUSTOMER"));

        mockMvc.perform(delete("/api/users/{id}/action-tokens", userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isForbidden());
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

