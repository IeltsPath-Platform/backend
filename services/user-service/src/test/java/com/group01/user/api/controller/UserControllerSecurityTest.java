package com.group01.user.api.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.group01.user.application.usecase.AssignRoleUseCase;
import com.group01.user.application.usecase.ChangeUserStatusUseCase;
import com.group01.user.application.usecase.CreateUserUseCase;
import com.group01.user.application.usecase.DeleteUserUseCase;
import com.group01.user.application.usecase.GetAllUsersUseCase;
import com.group01.user.application.usecase.GetMyProfileUseCase;
import com.group01.user.application.usecase.GetUserByIdUseCase;
import com.group01.user.application.usecase.RegisterUseCase;
import com.group01.user.application.usecase.UpdateUserUseCase;
import com.group01.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.vo.Email;
import com.group01.user.domain.vo.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "app.auth.external-jwt-issuer=urn:code-base:auth",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "app.security.public-endpoints[0].method=POST",
        "app.security.public-endpoints[0].patterns[0]=/api/users/register",
        "spring.cloud.config.enabled=false"
})
class UserControllerSecurityTest {
    private static final String EXTERNAL_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private static final String EXTERNAL_ISSUER = "urn:code-base:auth";
    private static final String INTERNAL_ISSUER = "urn:code-base:api-gateway";

    @Autowired
    private MockMvc mockMvc;

    @MockBean private RegisterUseCase registerUseCase;
    @MockBean private CreateUserUseCase createUserUseCase;
    @MockBean private GetMyProfileUseCase getMyProfileUseCase;
    @MockBean private GetUserByIdUseCase getUserByIdUseCase;
    @MockBean private GetAllUsersUseCase getAllUsersUseCase;
    @MockBean private UpdateUserUseCase updateUserUseCase;
    @MockBean private AssignRoleUseCase assignRoleUseCase;
    @MockBean private ChangeUserStatusUseCase changeUserStatusUseCase;
    @MockBean private DeleteUserUseCase deleteUserUseCase;

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.external-jwt-secret", () -> EXTERNAL_SECRET);
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
    }

    @Test
    void spoofedUserHeadersDoNotAuthenticateProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanReadOwnUserButCannotReadAnotherUser() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID foreignId = UUID.randomUUID();
        when(getUserByIdUseCase.execute(ownerId)).thenReturn(user(ownerId));
        String token = signedToken(INTERNAL_SECRET, ownerId, INTERNAL_ISSUER, List.of("LEARNER"));

        mockMvc.perform(get("/api/users/{id}", ownerId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/{id}", foreignId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListUsers() throws Exception {
        when(getAllUsersUseCase.execute()).thenReturn(List.of());

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + signedToken(INTERNAL_SECRET, UUID.randomUUID(),
                                        INTERNAL_ISSUER, List.of("ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void getMeUsesCurrentUserFromInternalToken() throws Exception {
        UUID userId = UUID.randomUUID();
        when(getMyProfileUseCase.execute(userId)).thenReturn(user(userId));

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + signedToken(INTERNAL_SECRET, userId,
                                        INTERNAL_ISSUER, List.of("LEARNER"))))
                .andExpect(status().isOk());

        verify(getMyProfileUseCase).execute(userId);
    }

    @Test
    void internalTokenWithWrongIssuerIsRejected() throws Exception {
        UUID subject = UUID.randomUUID();

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + signedToken(INTERNAL_SECRET, subject,
                                        "other-issuer", List.of("ADMIN"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void externalAccessTokenIsRejectedByUserService() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + signedToken(EXTERNAL_SECRET, UUID.randomUUID(),
                                        EXTERNAL_ISSUER, List.of("ADMIN"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalTokenWithoutSubjectIsRejected() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + signedTokenWithoutSubject(INTERNAL_SECRET,
                                        INTERNAL_ISSUER, List.of("ADMIN"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalTokenWithNonUuidSubjectIsRejected() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + signedToken(INTERNAL_SECRET, "not-a-uuid",
                                        INTERNAL_ISSUER, List.of("ADMIN"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalTokenWithUnsupportedRoleIsRejected() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + signedToken(INTERNAL_SECRET, UUID.randomUUID().toString(),
                                        INTERNAL_ISSUER, List.of("SUPER_ADMIN"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void configuredPublicRegisterEndpointDoesNotRequireBearerToken() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private String signedToken(
            String secret,
            UUID subject,
            String issuer,
            List<String> roles
    ) {
        return signedToken(secret, subject.toString(), issuer, roles);
    }

    private String signedToken(
            String secret,
            String subject,
            String issuer,
            List<String> roles
    ) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .expiresAt(now.plusSeconds(300))
                .claim("roles", roles)
                .build();
        return jwtEncoder(secret).encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private String signedTokenWithoutSubject(
            String secret,
            String issuer,
            List<String> roles
    ) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .expiresAt(now.plusSeconds(300))
                .claim("roles", roles)
                .build();
        return jwtEncoder(secret).encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private User user(UUID id) {
        return User.builder()
                .id(id)
                .email(new Email("owner@example.com"))
                .fullName("Owner")
                .status(UserStatus.ACTIVE)
                .build();
    }

    private static JwtEncoder jwtEncoder(String secret) {
        OctetSequenceKey key = new OctetSequenceKey.Builder(
                new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256"))
                .build();
        return new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(
                new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
    }
}
