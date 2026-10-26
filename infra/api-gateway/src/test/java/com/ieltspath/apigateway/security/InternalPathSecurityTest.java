package com.ieltspath.apigateway.security;

import com.ieltspath.apigateway.monitoring.ServiceHealth;
import com.ieltspath.apigateway.monitoring.ServiceHealthChecker;
import com.ieltspath.apigateway.monitoring.SystemServicesResponse;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

import static org.mockito.Mockito.when;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.auth.external-jwt-issuer=urn:code-base:auth",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
})
@AutoConfigureWebTestClient
@Import(InternalPathSecurityTest.PublicCatalogTestConfiguration.class)
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
    @MockBean ServiceHealthChecker serviceHealthChecker;

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

    @Test
    void onlyVocabularyCatalogGetRoutesArePublic() {
        client.get().uri("/api/content/vocabulary")
                .exchange().expectStatus().isOk();
        client.get().uri("/api/content/vocabulary/10000000-0000-4000-8000-000000000001")
                .exchange().expectStatus().isOk();
        client.get().uri("/api/content/vocabulary/suggestions?q=po").exchange().expectStatus().isOk();
        client.get().uri("/api/content/vocabulary/topics").exchange().expectStatus().isOk();
        client.get().uri("/api/content/vocabulary/sets").exchange().expectStatus().isOk();
        client.get().uri("/api/content/vocabulary/sets/10000000-0000-4000-8000-000000000001").exchange().expectStatus().isOk();
        client.get().uri("/api/content/vocabulary/sets/10000000-0000-4000-8000-000000000001/words").exchange().expectStatus().isOk();
        client.get().uri("/api/content/vocabulary/mindmap?itemId=10000000-0000-4000-8000-000000000001")
                .exchange().expectStatus().isOk();

        client.post().uri("/api/content/vocabulary").bodyValue("{}")
                .exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/content/vocabulary/search")
                .exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/content/vocabulary/senses?ids=10000000-0000-4000-8000-000000000001")
                .exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/content/admin/vocabulary/10000000-0000-4000-8000-000000000001")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void systemMonitoringRequiresAdminRole() throws Exception {
        client.get().uri("/api/admin/system/services")
                .exchange()
                .expectStatus().isUnauthorized();

        client.get().uri("/api/admin/system/services")
                .headers(headers -> headers.setBearerAuth(token("CUSTOMER")))
                .exchange()
                .expectStatus().isForbidden();

        client.get().uri("/api/admin/system/services")
                .headers(headers -> headers.setBearerAuth(token("CONTENT_AUTHOR")))
                .exchange()
                .expectStatus().isForbidden();

        when(serviceHealthChecker.checkServices()).thenReturn(Mono.just(new SystemServicesResponse(Instant.now(),
                List.of(new ServiceHealth("API-GATEWAY", com.ieltspath.apigateway.monitoring.HealthStatus.UP, List.of())))));
        client.get().uri("/api/admin/system/services")
                .headers(headers -> headers.setBearerAuth(token("ADMIN")))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.services[0].serviceId").isEqualTo("API-GATEWAY");
    }

    private String token(String role) {
        try {
            var claims = new JWTClaimsSet.Builder()
                    .issuer("urn:code-base:auth")
                    .subject(UUID.randomUUID().toString())
                    .expirationTime(java.util.Date.from(Instant.now().plusSeconds(300)))
                    .claim("roles", List.of(role))
                    .build();
            var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(EXTERNAL_KEY));
            return jwt.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @TestConfiguration
    static class PublicCatalogTestConfiguration {
        @Bean
        RouteLocator testPublicCatalogRoute(RouteLocatorBuilder builder) {
            return builder.routes()
                    .route("test-public-vocabulary", route -> route
                            .path("/api/content/vocabulary", "/api/content/vocabulary/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}",
                                    "/api/content/vocabulary/suggestions", "/api/content/vocabulary/topics", "/api/content/vocabulary/sets", "/api/content/vocabulary/mindmap",
                                    "/api/content/vocabulary/sets/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}",
                                    "/api/content/vocabulary/sets/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/words")
                            .and().method(HttpMethod.GET)
                            .filters(filters -> filters.setPath("/__test/catalog"))
                            .uri("forward:///"))
                    .build();
        }

        @Bean
        TestPublicCatalogController testPublicCatalogController() {
            return new TestPublicCatalogController();
        }
    }

    @org.springframework.web.bind.annotation.RestController
    static class TestPublicCatalogController {
        @org.springframework.web.bind.annotation.GetMapping("/__test/catalog")
        org.springframework.http.ResponseEntity<Void> catalog() {
            return org.springframework.http.ResponseEntity.ok().build();
        }
    }
}
