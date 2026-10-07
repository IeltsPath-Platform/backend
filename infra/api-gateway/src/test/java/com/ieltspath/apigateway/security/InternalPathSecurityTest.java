package com.ieltspath.apigateway.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.auth.external-jwt-issuer=urn:code-base:auth",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
})
@AutoConfigureWebTestClient
class InternalPathSecurityTest {
    private static final byte[] EXTERNAL_KEY = new byte[32];
    private static final byte[] INTERNAL_KEY = new byte[32];

    static {
        new SecureRandom().nextBytes(EXTERNAL_KEY);
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.external-jwt-secret", () -> Base64.getEncoder().encodeToString(EXTERNAL_KEY));
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    @Autowired WebTestClient client;

    @Test
    void authenticatedClientCannotAccessInternalPath() throws Exception {
        var claims = new JWTClaimsSet.Builder()
                .issuer("urn:code-base:auth")
                .subject(UUID.randomUUID().toString())
                .expirationTime(java.util.Date.from(Instant.now().plusSeconds(300)))
                .claim("roles", List.of("CUSTOMER"))
                .build();
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(EXTERNAL_KEY));

        client.get().uri("/internal/learning-content/topic-sequence")
                .headers(headers -> headers.setBearerAuth(jwt.serialize()))
                .exchange()
                .expectStatus().isForbidden();
    }
}
