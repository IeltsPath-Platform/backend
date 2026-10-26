package com.ieltspath.game.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.game.application.result.ActiveWordlyGameResult;
import com.ieltspath.game.application.usecase.ActiveWordlyGameUseCase;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
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

@WebMvcTest(WordlyActiveGameController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
class ActiveGameControllerTest {
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @Autowired MockMvc mvc;
    @MockBean ActiveWordlyGameUseCase activeGame;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Test
    void bothPathsReportTheRoomWithItsGameType() throws Exception {
        UUID room = UUID.randomUUID();
        when(activeGame.find(any())).thenReturn(Optional.of(
                new ActiveWordlyGameResult(room, "ABC123", "MULTIPLAYER", "WAITING", null, null, "ROCKET")));
        for (String path : List.of("/api/games/me/active-game", "/api/games/wordly/me/active-game")) {
            mvc.perform(get(path).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(room.toString()))
                .andExpect(jsonPath("$.gameType").value("ROCKET"))
                .andExpect(jsonPath("$.roomStatus").value("WAITING"));
        }
    }

    @Test
    void noRoomIsNoContentAndLeaveIsNoContent() throws Exception {
        when(activeGame.find(any())).thenReturn(Optional.empty());
        mvc.perform(get("/api/games/me/active-game").header("Authorization", bearer())).andExpect(status().isNoContent());
        mvc.perform(post("/api/games/me/active-game/leave").header("Authorization", bearer()))
            .andExpect(status().isNoContent());
        verify(activeGame).leave(any());
        mvc.perform(get("/api/games/me/active-game")).andExpect(status().isUnauthorized());
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
