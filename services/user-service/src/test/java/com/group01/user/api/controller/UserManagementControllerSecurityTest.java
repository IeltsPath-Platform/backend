package com.group01.user.api.controller;

import com.group01.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.group01.user.application.command.AssignRoleCommand;
import com.group01.user.application.command.ChangeLearningGoalStatusCommand;
import com.group01.user.application.command.ChangeUserStatusCommand;
import com.group01.user.application.command.CreateUserCommand;
import com.group01.user.application.command.UpdateLearnerProfileCommand;
import com.group01.user.application.command.UpdateUserCommand;
import com.group01.user.application.result.AccountActionTokenResult;
import com.group01.user.application.result.LearnerProfileResult;
import com.group01.user.application.result.LearningGoalResult;
import com.group01.user.application.result.OAuthIdentityResult;
import com.group01.user.application.usecase.AssignRoleUseCase;
import com.group01.user.application.usecase.ChangeLearningGoalStatusUseCase;
import com.group01.user.application.usecase.ChangeUserStatusUseCase;
import com.group01.user.application.usecase.CreateUserUseCase;
import com.group01.user.application.usecase.DeleteLearnerProfileUseCase;
import com.group01.user.application.usecase.DeleteUserUseCase;
import com.group01.user.application.usecase.GetActionTokensByUserIdUseCase;
import com.group01.user.application.usecase.GetAllUsersUseCase;
import com.group01.user.application.usecase.GetLearnerProfileUseCase;
import com.group01.user.application.usecase.GetLearningGoalsByUserIdUseCase;
import com.group01.user.application.usecase.GetOAuthIdentitiesUseCase;
import com.group01.user.application.usecase.GetUserByIdUseCase;
import com.group01.user.application.usecase.RevokeUserActionTokensUseCase;
import com.group01.user.application.usecase.UnlinkOAuthIdentityUseCase;
import com.group01.user.application.usecase.UpdateLearnerProfileUseCase;
import com.group01.user.application.usecase.UpdateUserUseCase;
import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.vo.Email;
import com.group01.user.domain.vo.UserStatus;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserManagementController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
    "app.auth.external-jwt-issuer=urn:code-base:auth",
    "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
    "spring.cloud.config.enabled=false"
})
class UserManagementControllerSecurityTest {

    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private static final String INTERNAL_ISSUER = "urn:code-base:api-gateway";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetAllUsersUseCase getAllUsersUseCase;
    @MockBean
    private GetUserByIdUseCase getUserByIdUseCase;
    @MockBean
    private CreateUserUseCase createUserUseCase;
    @MockBean
    private UpdateUserUseCase updateUserUseCase;
    @MockBean
    private AssignRoleUseCase assignRoleUseCase;
    @MockBean
    private ChangeUserStatusUseCase changeUserStatusUseCase;
    @MockBean
    private DeleteUserUseCase deleteUserUseCase;

    @MockBean
    private GetLearnerProfileUseCase getLearnerProfileUseCase;
    @MockBean
    private UpdateLearnerProfileUseCase updateLearnerProfileUseCase;
    @MockBean
    private DeleteLearnerProfileUseCase deleteLearnerProfileUseCase;

    @MockBean
    private GetLearningGoalsByUserIdUseCase getLearningGoalsByUserIdUseCase;
    @MockBean
    private ChangeLearningGoalStatusUseCase changeLearningGoalStatusUseCase;

    @MockBean
    private GetOAuthIdentitiesUseCase getOAuthIdentitiesUseCase;
    @MockBean
    private UnlinkOAuthIdentityUseCase unlinkOAuthIdentityUseCase;

    @MockBean
    private GetActionTokensByUserIdUseCase getActionTokensByUserIdUseCase;
    @MockBean
    private RevokeUserActionTokensUseCase revokeUserActionTokensUseCase;

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotAccessAnyAdminEndpoint() throws Exception {
        UUID userId = UUID.randomUUID();
        String customerToken = signedToken(INTERNAL_SECRET, userId, INTERNAL_ISSUER, List.of("CUSTOMER"));

        mockMvc.perform(get("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/{id}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/{id}/profile", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/{id}/learning-goals", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/{id}/oauth", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/{id}/action-tokens", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListUsers() throws Exception {
        when(getAllUsersUseCase.execute()).thenReturn(List.of());
        String adminToken = signedToken(INTERNAL_SECRET, UUID.randomUUID(), INTERNAL_ISSUER, List.of("ADMIN"));

        mockMvc.perform(get("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanCreateUser() throws Exception {
        UUID newUserId = UUID.randomUUID();
        when(createUserUseCase.execute(any(CreateUserCommand.class))).thenReturn(user(newUserId));
        String adminToken = signedToken(INTERNAL_SECRET, UUID.randomUUID(), INTERNAL_ISSUER, List.of("ADMIN"));

        String body = """
                {
                    "email": "newuser@example.com",
                    "password": "Password123!",
                    "fullName": "New User",
                    "phoneNumber": "0987654321",
                    "roles": ["CUSTOMER"]
                }
                """;

        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(newUserId.toString()));
    }

    @Test
    void adminCanGetUserById() throws Exception {
        UUID targetId = UUID.randomUUID();
        when(getUserByIdUseCase.execute(targetId)).thenReturn(user(targetId));
        String adminToken = signedToken(INTERNAL_SECRET, UUID.randomUUID(), INTERNAL_ISSUER, List.of("ADMIN"));

        mockMvc.perform(get("/api/users/{id}", targetId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetId.toString()));
    }

    @Test
    void adminCanUpdateUser() throws Exception {
        UUID targetId = UUID.randomUUID();
        when(updateUserUseCase.execute(any(UpdateUserCommand.class))).thenReturn(user(targetId));
        String adminToken = signedToken(INTERNAL_SECRET, UUID.randomUUID(), INTERNAL_ISSUER, List.of("ADMIN"));

        String body = """
                {
                    "fullName": "Updated Admin User",
                    "phoneNumber": "0987654321"
                }
                """;

        mockMvc.perform(put("/api/users/{id}", targetId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanAssignRoles() throws Exception {
        UUID targetId = UUID.randomUUID();
        when(assignRoleUseCase.execute(any(AssignRoleCommand.class))).thenReturn(user(targetId));
        String adminToken = signedToken(INTERNAL_SECRET, UUID.randomUUID(), INTERNAL_ISSUER, List.of("ADMIN"));

        String body = """
                {
                    "roles": ["CUSTOMER", "EXAMINER"]
                }
                """;

        mockMvc.perform(put("/api/users/{id}/roles", targetId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanChangeUserStatus() throws Exception {
        UUID targetId = UUID.randomUUID();
        when(changeUserStatusUseCase.execute(any(ChangeUserStatusCommand.class))).thenReturn(user(targetId));
        String adminToken = signedToken(INTERNAL_SECRET, UUID.randomUUID(), INTERNAL_ISSUER, List.of("ADMIN"));

        String body = """
                {
                    "status": "LOCKED"
                }
                """;

        mockMvc.perform(put("/api/users/{id}/status", targetId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanDeleteUser() throws Exception {
        UUID targetId = UUID.randomUUID();
        when(deleteUserUseCase.execute(targetId)).thenReturn(user(targetId));
        String adminToken = signedToken(INTERNAL_SECRET, UUID.randomUUID(), INTERNAL_ISSUER, List.of("ADMIN"));

        mockMvc.perform(delete("/api/users/{id}", targetId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanManageLearnerProfile() throws Exception {
        UUID targetId = UUID.randomUUID();
        when(getLearnerProfileUseCase.execute(targetId)).thenReturn(profileResult(targetId, "Target Learner"));
        when(updateLearnerProfileUseCase.execute(any(UpdateLearnerProfileCommand.class)))
                .thenReturn(profileResult(targetId, "Admin Updated"));
        String adminToken = signedToken(INTERNAL_SECRET, UUID.randomUUID(), INTERNAL_ISSUER, List.of("ADMIN"));

        mockMvc.perform(get("/api/users/{id}/profile", targetId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Target Learner"));

        String body = """
                {
                    "displayName": "Admin Updated",
                    "visibility": "PUBLIC"
                }
                """;

        mockMvc.perform(put("/api/users/{id}/profile", targetId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/users/{id}/profile", targetId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa hồ sơ học tập thành công"));

        verify(deleteLearnerProfileUseCase).execute(targetId);
    }

    @Test
    void adminCanManageLearningGoals() throws Exception {
        UUID targetId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(getLearningGoalsByUserIdUseCase.execute(targetId)).thenReturn(List.of(goalResult(targetId, BigDecimal.valueOf(7.0))));
        when(changeLearningGoalStatusUseCase.execute(any(ChangeLearningGoalStatusCommand.class)))
                .thenReturn(goalResult(targetId, BigDecimal.valueOf(7.0)));
        String adminToken = signedToken(INTERNAL_SECRET, UUID.randomUUID(), INTERNAL_ISSUER, List.of("ADMIN"));

        mockMvc.perform(get("/api/users/{id}/learning-goals", targetId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        String body = """
                {
                    "status": "ACHIEVED"
                }
                """;

        mockMvc.perform(put("/api/users/{id}/learning-goals/{goalId}/status", targetId, goalId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanManageOAuthIdentities() throws Exception {
        UUID targetId = UUID.randomUUID();
        when(getOAuthIdentitiesUseCase.execute(targetId)).thenReturn(List.of());
        String adminToken = signedToken(INTERNAL_SECRET, UUID.randomUUID(), INTERNAL_ISSUER, List.of("ADMIN"));

        mockMvc.perform(get("/api/users/{id}/oauth", targetId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/users/{id}/oauth/{provider}", targetId, "GOOGLE")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hủy liên kết tài khoản OAuth của người dùng thành công"));

        verify(unlinkOAuthIdentityUseCase).execute(targetId, "GOOGLE");
    }

    @Test
    void adminCanManageActionTokens() throws Exception {
        UUID targetId = UUID.randomUUID();
        when(getActionTokensByUserIdUseCase.execute(targetId)).thenReturn(List.of(
                new AccountActionTokenResult(UUID.randomUUID(), targetId, "PASSWORD_RESET",
                        LocalDateTime.now().plusMinutes(15), null, LocalDateTime.now())
        ));
        String adminToken = signedToken(INTERNAL_SECRET, UUID.randomUUID(), INTERNAL_ISSUER, List.of("ADMIN"));

        mockMvc.perform(get("/api/users/{id}/action-tokens", targetId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].purpose").value("PASSWORD_RESET"));

        mockMvc.perform(delete("/api/users/{id}/action-tokens", targetId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Thu hồi toàn bộ token của người dùng thành công"));

        verify(revokeUserActionTokensUseCase).execute(targetId);
    }

    private User user(UUID id) {
        return User.builder()
                .id(id)
                .email(new Email("user@example.com"))
                .fullName("User Name")
                .status(UserStatus.ACTIVE)
                .roles(Set.of())
                .build();
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

    private LearningGoalResult goalResult(UUID userId, BigDecimal targetBand) {
        return new LearningGoalResult(
                UUID.randomUUID(),
                userId,
                targetBand,
                LocalDate.now().plusMonths(3),
                60,
                "ACTIVE",
                LocalDateTime.now(),
                null,
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
