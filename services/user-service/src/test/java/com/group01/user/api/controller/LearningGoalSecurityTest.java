package com.group01.user.api.controller;

import com.group01.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.group01.user.application.command.ChangeLearningGoalStatusCommand;
import com.group01.user.application.command.CreateLearningGoalCommand;
import com.group01.user.application.result.LearningGoalResult;
import com.group01.user.application.usecase.ChangeLearningGoalStatusUseCase;
import com.group01.user.application.usecase.CreateLearningGoalUseCase;
import com.group01.user.application.usecase.GetActiveLearningGoalUseCase;
import com.group01.user.application.usecase.GetLearningGoalsByUserIdUseCase;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LearningGoalController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
    "app.auth.external-jwt-issuer=urn:code-base:auth",
    "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
    "spring.cloud.config.enabled=false"
})
class LearningGoalSecurityTest {

    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private static final String INTERNAL_ISSUER = "urn:code-base:api-gateway";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetActiveLearningGoalUseCase getActiveLearningGoalUseCase;
    @MockBean
    private GetLearningGoalsByUserIdUseCase getLearningGoalsByUserIdUseCase;
    @MockBean
    private CreateLearningGoalUseCase createLearningGoalUseCase;
    @MockBean
    private ChangeLearningGoalStatusUseCase changeLearningGoalStatusUseCase;

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me/learning-goals/active"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void learnerCanGetOwnActiveGoal() throws Exception {
        UUID userId = UUID.randomUUID();
        when(getActiveLearningGoalUseCase.execute(userId)).thenReturn(goalResult(userId, BigDecimal.valueOf(7.5)));

        String token = signedToken(INTERNAL_SECRET, userId, INTERNAL_ISSUER, List.of("CUSTOMER"));

        mockMvc.perform(get("/api/users/me/learning-goals/active")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetBand").value(7.5));

        verify(getActiveLearningGoalUseCase).execute(userId);
    }

    @Test
    void learnerCanGetOwnLearningGoals() throws Exception {
        UUID userId = UUID.randomUUID();
        when(getLearningGoalsByUserIdUseCase.execute(userId)).thenReturn(List.of(goalResult(userId, BigDecimal.valueOf(7.0))));

        String token = signedToken(INTERNAL_SECRET, userId, INTERNAL_ISSUER, List.of("CUSTOMER"));

        mockMvc.perform(get("/api/users/me/learning-goals")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetBand").value(7.0));

        verify(getLearningGoalsByUserIdUseCase).execute(userId);
    }

    @Test
    void learnerCanCreateGoal() throws Exception {
        UUID userId = UUID.randomUUID();
        when(createLearningGoalUseCase.execute(any(CreateLearningGoalCommand.class)))
                .thenReturn(goalResult(userId, BigDecimal.valueOf(8.0)));

        String token = signedToken(INTERNAL_SECRET, userId, INTERNAL_ISSUER, List.of("CUSTOMER"));

        String body = """
                {
                    "targetBand": 8.0,
                    "examDate": "2026-12-31",
                    "availableMinutesPerDay": 60
                }
                """;

        mockMvc.perform(post("/api/users/me/learning-goals")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetBand").value(8.0));

        verify(createLearningGoalUseCase).execute(any(CreateLearningGoalCommand.class));
    }

    @Test
    void learnerCanUpdateOwnGoalStatus() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(changeLearningGoalStatusUseCase.execute(any(ChangeLearningGoalStatusCommand.class)))
                .thenReturn(goalResult(userId, BigDecimal.valueOf(7.0)));

        String token = signedToken(INTERNAL_SECRET, userId, INTERNAL_ISSUER, List.of("CUSTOMER"));

        String body = """
                {
                    "status": "ACHIEVED"
                }
                """;

        mockMvc.perform(put("/api/users/me/learning-goals/{goalId}/status", goalId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());

        verify(changeLearningGoalStatusUseCase).execute(any(ChangeLearningGoalStatusCommand.class));
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
