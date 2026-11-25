package com.ieltspath.apigateway.security;

import com.ieltspath.apigateway.monitoring.ServiceHealthChecker;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * A 401 must reach the browser with CORS headers whatever caused it. Without them the browser hides the response and
 * the web app sees a network error instead of a 401, so it never tries to refresh the access token.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.auth.external-jwt-issuer=urn:code-base:auth",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
})
@AutoConfigureWebTestClient
class UnauthorizedResponseCorsTest {
    private static final byte[] EXTERNAL_KEY = new byte[32];
    private static final byte[] INTERNAL_KEY = new byte[32];
    private static final String WEB_ORIGIN = "http://localhost:5173";
    private static final String PROTECTED_PATH = "/api/content/courses";

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
    @MockBean ServiceHealthChecker serviceHealthChecker;

    @Test
    void anExpiredTokenGetsA401WithCorsHeaders() throws Exception {
        String expired = token(EXTERNAL_KEY, Instant.now().minusSeconds(600));

        client.get().uri(PROTECTED_PATH)
                .header("Origin", WEB_ORIGIN)
                .headers(headers -> headers.setBearerAuth(expired))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", WEB_ORIGIN)
                .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true");
    }

    @Test
    void aTokenThatIsNotAJwtGetsA401WithCorsHeaders() {
        client.get().uri(PROTECTED_PATH)
                .header("Origin", WEB_ORIGIN)
                .headers(headers -> headers.setBearerAuth("not.a.token"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", WEB_ORIGIN);
    }

    @Test
    void aTokenSignedWithAnotherKeyGetsA401WithCorsHeaders() throws Exception {
        byte[] otherKey = new byte[32];
        new SecureRandom().nextBytes(otherKey);
        String forged = token(otherKey, Instant.now().plusSeconds(300));

        client.get().uri(PROTECTED_PATH)
                .header("Origin", WEB_ORIGIN)
                .headers(headers -> headers.setBearerAuth(forged))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", WEB_ORIGIN);
    }

    @Test
    void aRequestWithoutATokenStillGetsA401WithCorsHeaders() {
        client.get().uri(PROTECTED_PATH)
                .header("Origin", WEB_ORIGIN)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", WEB_ORIGIN);
    }

    @Test
    void anOriginThatIsNotAllowedGetsNoCorsHeadersEvenOnA401() throws Exception {
        String expired = token(EXTERNAL_KEY, Instant.now().minusSeconds(600));

        client.get().uri(PROTECTED_PATH)
                .header("Origin", "https://untrusted.example")
                .headers(headers -> headers.setBearerAuth(expired))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().doesNotExist("Access-Control-Allow-Origin");
    }

    @Test
    void theErrorBodyIsTheGatewaysJsonShapeForAnExpiredToken() throws Exception {
        String expired = token(EXTERNAL_KEY, Instant.now().minusSeconds(600));

        client.get().uri(PROTECTED_PATH)
                .header("Origin", WEB_ORIGIN)
                .headers(headers -> headers.setBearerAuth(expired))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.path").isEqualTo(PROTECTED_PATH);
    }

    private static String token(byte[] key, Instant expiresAt) throws Exception {
        var claims = new JWTClaimsSet.Builder()
                .issuer("urn:code-base:auth")
                .subject(UUID.randomUUID().toString())
                .expirationTime(Date.from(expiresAt))
                .claim("roles", List.of("CONTENT_AUTHOR"))
                .build();
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(key));
        return jwt.serialize();
    }
}
