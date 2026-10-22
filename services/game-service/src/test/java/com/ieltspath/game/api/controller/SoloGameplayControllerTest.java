package com.ieltspath.game.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.game.application.usecase.SoloGameplayUseCase;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(SoloGameplayController.class)

@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
class SoloGameplayControllerTest {
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @Autowired
    private MockMvc mvc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @MockBean SoloGameplayUseCase gameplay;
    @MockBean com.ieltspath.game.application.usecase.RocketGameplayUseCase rocket;
    @Test void protectedEndpointsRequireAuthentication() throws Exception {
        String base="/api/games/matches/"+UUID.randomUUID();
        mvc.perform(post(base+"/leave")).andExpect(status().isUnauthorized());
        mvc.perform(get(base+"/result")).andExpect(status().isUnauthorized());
        mvc.perform(post(base+"/guesses").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(gameplay);
    }
    @Test void invalidGuessBodyDoesNotReachApplication() throws Exception {
        mvc.perform(post("/api/games/matches/"+UUID.randomUUID()+"/guesses").contentType("application/json")
                .header("Authorization","Bearer "+token("urn:code-base:api-gateway",Instant.now().plusSeconds(300))).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(gameplay);
    }
    @Test void validGuessCallsApplicationWithVerifiedIdentity() throws Exception {
        UUID match=UUID.randomUUID(),word=UUID.randomUUID(),request=UUID.randomUUID();
        String token=token("urn:code-base:api-gateway",Instant.now().plusSeconds(300));
        String subject=com.nimbusds.jwt.SignedJWT.parse(token).getJWTClaimsSet().getSubject();
        when(gameplay.submit(any(),any(),any(),any(),any())).thenReturn(new com.ieltspath.game.application.result.SoloReply(200,java.util.Map.of("wordStatus","ACTIVE")));
        mvc.perform(post("/api/games/matches/"+match+"/guesses").contentType("application/json")
                .header("Authorization","Bearer "+token).header("X-User-Id",UUID.randomUUID().toString())
                .content("{\"requestId\":\""+request+"\",\"playerWordId\":\""+word+"\",\"guess\":\"AIR\"}"))
                .andExpect(status().isOk());
        verify(gameplay).submit(match,word,UUID.fromString(subject),request,"AIR");
    }
    @Test void leaveAndResultOfRocketMatchesGoToRocket() throws Exception {
        UUID match=UUID.randomUUID();
        String token="Bearer "+token("urn:code-base:api-gateway",Instant.now().plusSeconds(300));
        when(rocket.isRocket(match)).thenReturn(true);
        when(rocket.leave(eq(match),any())).thenReturn(new com.ieltspath.game.application.result.RocketGameplayResults.Left(
                match,"COMPLETED",com.ieltspath.game.domain.vo.RocketEndReason.PLAYER_LEFT,"FORFEITED",150,true));
        mvc.perform(post("/api/games/matches/"+match+"/leave").header("Authorization",token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.endReason").value("PLAYER_LEFT"));
        mvc.perform(get("/api/games/matches/"+match+"/result").header("Authorization",token)).andExpect(status().isOk());
        verify(rocket).result(eq(match),any());
        verifyNoInteractions(gameplay);
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
