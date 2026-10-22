package com.ieltspath.game.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.game.application.result.GameRoomResult;
import com.ieltspath.game.application.result.RocketGameplayResults;
import com.ieltspath.game.application.usecase.CreateWordlyRoomUseCase;
import com.ieltspath.game.application.usecase.RocketGameplayUseCase;
import com.ieltspath.game.domain.aggregate.GameRoom;
import com.ieltspath.game.domain.exception.SoloGameplayException;
import com.ieltspath.game.domain.vo.RocketRoomSettings;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RocketGameController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
class RocketGameControllerTest {
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @Autowired MockMvc mvc;
    @MockBean CreateWordlyRoomUseCase rooms;
    @MockBean RocketGameplayUseCase gameplay;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Test
    void createsSoloRoomsWithTheChosenQuestionCount() throws Exception {
        UUID host = UUID.randomUUID();
        var room = GameRoom.open(UUID.randomUUID(), "ABC123", host, RocketRoomSettings.solo(8), Instant.now());
        when(rooms.open(any(), any())).thenReturn(GameRoomResult.from(room));
        mvc.perform(post("/api/games/rocket/rooms").header("Authorization", bearer()).contentType("application/json")
                        .content("{\"playerMode\":\"SOLO\",\"questionCount\":8}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameType").value("ROCKET"))
                .andExpect(jsonPath("$.playerMode").value("SOLO"))
                .andExpect(jsonPath("$.maxPlayers").value(1));
        verify(rooms).open(any(), eq(RocketRoomSettings.solo(8)));
    }

    @Test
    void createsAMultiplayerRoomWithTheHostsSettings() throws Exception {
        var settings = RocketRoomSettings.multiplayer(12, 5, 20);
        var room = GameRoom.open(UUID.randomUUID(), "ABC123", UUID.randomUUID(), settings, Instant.now());
        when(rooms.open(any(), any())).thenReturn(GameRoomResult.from(room));
        mvc.perform(post("/api/games/rocket/rooms").header("Authorization", bearer()).contentType("application/json")
                        .content("{\"playerMode\":\"MULTIPLAYER\",\"questionCount\":12,\"initialLives\":5,\"secondsPerQuestion\":20}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.playerMode").value("MULTIPLAYER"))
                .andExpect(jsonPath("$.maxPlayers").value(4))
                .andExpect(jsonPath("$.settings.questionCount").value(12))
                .andExpect(jsonPath("$.settings.initialLives").value(5))
                .andExpect(jsonPath("$.settings.secondsPerQuestion").value(20));
        verify(rooms).open(any(), eq(settings));
        mvc.perform(post("/api/games/rocket/rooms").header("Authorization", bearer()).contentType("application/json")
                .content("{\"playerMode\":\"MULTIPLAYER\",\"questionCount\":10}")).andExpect(status().isCreated());
        verify(rooms).open(any(), eq(RocketRoomSettings.multiplayer(10, 4, 15)));
    }

    @Test
    void rejectsInvalidSettingsAndMissingAuthentication() throws Exception {
        mvc.perform(post("/api/games/rocket/rooms").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        for (String body : List.of("{\"playerMode\":\"SOLO\",\"questionCount\":2}",
                "{\"playerMode\":\"SOLO\",\"questionCount\":21}", "{\"playerMode\":\"SOLO\"}",
                "{\"playerMode\":\"MULTIPLAYER\",\"questionCount\":4}",
                "{\"playerMode\":\"MULTIPLAYER\",\"questionCount\":10,\"initialLives\":7}",
                "{\"playerMode\":\"MULTIPLAYER\",\"questionCount\":10,\"secondsPerQuestion\":12}")) {
            mvc.perform(post("/api/games/rocket/rooms").header("Authorization", bearer()).contentType("application/json")
                    .content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(rooms);
    }

    @Test
    void roundRoutesPassThroughAndMapErrors() throws Exception {
        UUID match = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        UUID option = UUID.randomUUID();
        when(gameplay.current(eq(match), any())).thenReturn(new RocketGameplayResults.Current(match,
                new RocketGameplayResults.Settings(6, 4, 15), null, null, true, Instant.parse("2026-10-22T00:00:00Z")));
        mvc.perform(get("/api/games/rocket/matches/" + match + "/rounds/current").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchFinished").value(true))
                .andExpect(jsonPath("$.settings.questionCount").value(6));

        when(gameplay.answer(eq(match), eq(round), any(), eq("r1"), eq(option)))
                .thenThrow(new SoloGameplayException(409, "ROUND_CLOSED", "This round is closed"));
        mvc.perform(post("/api/games/rocket/matches/" + match + "/rounds/" + round + "/answers")
                        .header("Authorization", bearer()).contentType("application/json")
                        .content("{\"requestId\":\"r1\",\"optionId\":\"" + option + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROUND_CLOSED"));
        mvc.perform(post("/api/games/rocket/matches/" + match + "/rounds/" + round + "/answers")
                        .header("Authorization", bearer()).contentType("application/json")
                        .content("{\"requestId\":\"\",\"optionId\":\"" + option + "\"}"))
                .andExpect(status().isBadRequest());
    }

    private String bearer() {
        var claims = JwtClaimsSet.builder().issuer("urn:code-base:api-gateway").subject(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusSeconds(300)).claim("roles", List.of("CUSTOMER")).build();
        var key = new OctetSequenceKey.Builder(KEY).build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
