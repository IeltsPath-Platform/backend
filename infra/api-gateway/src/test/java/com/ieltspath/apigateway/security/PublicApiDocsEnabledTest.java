package com.ieltspath.apigateway.security;

import com.ieltspath.apigateway.monitoring.ServiceHealthChecker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.security.SecureRandom;
import java.util.Base64;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true",
        "app.auth.external-jwt-issuer=urn:code-base:auth",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
})
@AutoConfigureWebTestClient
class PublicApiDocsEnabledTest {
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
    void apiDocumentationIsPublicWhenEnabled() {
        client.get().uri("/swagger-ui.html").exchange().expectStatus().is3xxRedirection();
        client.get().uri("/v3/api-docs").exchange().expectStatus().isOk();
    }
}
