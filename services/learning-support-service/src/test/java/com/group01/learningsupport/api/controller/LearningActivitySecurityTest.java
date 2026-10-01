package com.group01.learningsupport.api.controller;

import com.group01.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.group01.learningsupport.application.command.CreateLearningActivityCommand;
import com.group01.learningsupport.application.result.LearningActivityResult;
import com.group01.learningsupport.application.usecase.CreateLearningActivityUseCase;
import com.group01.learningsupport.application.usecase.DeleteLearningActivityUseCase;
import com.group01.learningsupport.application.usecase.ListLearningActivitiesUseCase;
import com.group01.learningsupport.domain.aggregate.LearningActivity;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LearningActivityController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "app.auth.external-jwt-issuer=urn:code-base:auth",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class LearningActivitySecurityTest {
    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private static final String INTERNAL_ISSUER = "urn:code-base:api-gateway";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateLearningActivityUseCase createLearningActivityUseCase;
    @MockBean
    private ListLearningActivitiesUseCase listLearningActivitiesUseCase;
    @MockBean
    private DeleteLearningActivityUseCase deleteLearningActivityUseCase;

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
    }

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/learning-support/activities"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSubjectIsPassedToCreateActivity() throws Exception {
        UUID userId = UUID.randomUUID();
        when(createLearningActivityUseCase.execute(any()))
                .thenReturn(LearningActivityResult.from(activity(userId)));

        mockMvc.perform(post("/api/learning-support/activities")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + signedToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activityType": "WATCH",
                                  "sourceType": "VIDEO",
                                  "sourceId": "11111111-1111-1111-1111-111111111111",
                                  "occurredAt": "2026-09-23T04:00:00Z",
                                  "durationSeconds": 12
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.activityType").value("WATCH"))
                .andExpect(jsonPath("$.durationSeconds").value(12));

        ArgumentCaptor<CreateLearningActivityCommand> captor = ArgumentCaptor.forClass(CreateLearningActivityCommand.class);
        verify(createLearningActivityUseCase).execute(captor.capture());
        assertEquals(userId, captor.getValue().userId());
    }

    private static LearningActivity activity(UUID userId) {
        return LearningActivity.create(
                userId,
                "WATCH",
                "VIDEO",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                Instant.parse("2026-09-23T04:00:00Z"),
                12
        );
    }

    private static String signedToken(UUID subject) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(INTERNAL_ISSUER)
                .subject(subject.toString())
                .expiresAt(now.plusSeconds(300))
                .claim("roles", List.of("CUSTOMER"))
                .build();
        OctetSequenceKey key = new OctetSequenceKey.Builder(
                new SecretKeySpec(Base64.getDecoder().decode(INTERNAL_SECRET), "HmacSHA256"))
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
