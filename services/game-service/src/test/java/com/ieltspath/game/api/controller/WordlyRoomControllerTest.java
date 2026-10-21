package com.ieltspath.game.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.game.application.usecase.CreateWordlyRoomUseCase;
import com.ieltspath.game.domain.repository.GameRoomRepository;
import com.ieltspath.game.application.port.OutboxWriter;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WordlyRoomController.class)
@Import(CreateWordlyRoomUseCase.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
class WordlyRoomControllerTest {
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @Autowired
    private MockMvc mvc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @MockBean com.ieltspath.game.application.port.CurrentPlayerProfile profile;
    @MockBean GameRoomRepository rooms;
    @MockBean OutboxWriter outbox;
    @MockBean com.ieltspath.game.application.usecase.WordlyRoomAdmission admission;

    @Test
    void rejectsCreatingAnotherRoomWhileTheUserStillBelongsToOne() throws Exception {
        doThrow(new com.ieltspath.game.domain.exception.SoloGameplayException(409, "ACTIVE_GAME_EXISTS",
                "You are already in a Wordly room")).when(admission).requireNoActiveGame(any());
        mvc.perform(post("/api/games/wordly/rooms")
                .header("Authorization", "Bearer " + token("urn:code-base:api-gateway", Instant.now().plusSeconds(300)))
                .contentType("application/json")
                .content("{\"playerMode\":\"SOLO\",\"initialLives\":4,\"playMode\":\"FINITE\",\"wordLimit\":10}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ACTIVE_GAME_EXISTS"));
        verify(rooms, never()).save(any());
    }

    @Test
    void createsSoloAndMultiplayerRoomsWithVerifiedHostAndSettings() throws Exception {
        when(profile.displayName()).thenReturn("Demo Learner");
        when(rooms.findByCode(any())).thenReturn(java.util.Optional.empty());
        when(rooms.save(any())).thenAnswer(call -> call.getArgument(0));
        for (String mode : List.of("SOLO", "MULTIPLAYER")) {
            mvc.perform(post("/api/games/wordly/rooms")
                    .header("Authorization", "Bearer " + token("urn:code-base:api-gateway", Instant.now().plusSeconds(300)))
                    .contentType("application/json")
                    .content("{\"playerMode\":\"" + mode + "\",\"initialLives\":4,\"playMode\":\"FINITE\",\"wordLimit\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode").value(org.hamcrest.Matchers.matchesPattern("[A-Z0-9]{6}")))
                .andExpect(jsonPath("$.playerMode").value(mode))
                .andExpect(jsonPath("$.maxPlayers").value(mode.equals("SOLO") ? 1 : 4))
                .andExpect(jsonPath("$.settings.wordTimeLimitSeconds").value(180))
                .andExpect(jsonPath("$.settings.wordLimit").value(10))
                .andExpect(jsonPath("$.members[0].displayName").value("Demo Learner"))
                .andExpect(jsonPath("$.members[0].role").value("HOST"))
                .andExpect(jsonPath("$.members[0].slotNumber").value(1));
        }
        verify(rooms, times(2)).save(any());
    }

    @Test
    void rejectsInvalidSettingsAndMissingAuthentication() throws Exception {
        mvc.perform(post("/api/games/wordly/rooms").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
        for (String body : List.of("{}",
                "{\"playerMode\":\"INVALID\",\"initialLives\":4,\"playMode\":\"FINITE\",\"wordLimit\":10}",
                "{\"playerMode\":\"SOLO\",\"initialLives\":4,\"playMode\":\"ENDLESS\",\"wordLimit\":10}")) {
            mvc.perform(post("/api/games/wordly/rooms").contentType("application/json")
                .header("Authorization", "Bearer " + token("urn:code-base:api-gateway", Instant.now().plusSeconds(300)))
                .content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(rooms, outbox);
    }

    private String token(String issuer, Instant expiresAt) {
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(UUID.randomUUID().toString())
                .expiresAt(expiresAt).claim("roles", List.of("CUSTOMER")).build();
        var key = new OctetSequenceKey.Builder(KEY).build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
