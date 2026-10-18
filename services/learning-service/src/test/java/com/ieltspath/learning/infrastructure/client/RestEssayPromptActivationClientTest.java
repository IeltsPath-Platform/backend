package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.learning.application.exception.LearningRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RestEssayPromptActivationClientTest {
    private MockRestServiceServer server;
    private RestEssayPromptActivationClient client;

    @BeforeEach void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://assessment.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestEssayPromptActivationClient(builder.build());
        Jwt jwt = Jwt.withTokenValue("internal-test-token").header("alg", "HS256")
                .subject(UUID.randomUUID().toString()).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, java.util.List.of()));
    }

    @AfterEach void tearDown() {
        SecurityContextHolder.clearContext();
        server.verify();
    }

    @Test void confirmedMissingActivationEnablesBootstrap() {
        server.expect(requestTo("http://assessment.test/internal/assessments/ai-prompt-activations?task=TASK_1"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer internal-test-token"))
                .andRespond(withSuccess("{\"task\":\"TASK_1\",\"promptVersion\":null}", MediaType.APPLICATION_JSON));
        assertNull(client.activatedVersion("TASK_1"));
    }

    @Test void returnsTheActivatedVersionForTheRequestedTask() {
        server.expect(requestTo("http://assessment.test/internal/assessments/ai-prompt-activations?task=TASK_2"))
                .andRespond(withSuccess("{\"task\":\"TASK_2\",\"promptVersion\":\"RUBRIC@2+tplv1\"}", MediaType.APPLICATION_JSON));
        assertEquals("RUBRIC@2+tplv1", client.activatedVersion("TASK_2"));
    }

    @Test void outageCannotBeMistakenForMissingActivation() {
        server.expect(requestTo("http://assessment.test/internal/assessments/ai-prompt-activations?task=TASK_2"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertEquals("PROMPT_VERSION_UNAVAILABLE", assertThrows(LearningRequestException.class,
                () -> client.activatedVersion("TASK_2")).getCode());
    }

    @Test void anotherTasksResponseCannotEnableBootstrap() {
        server.expect(requestTo("http://assessment.test/internal/assessments/ai-prompt-activations?task=TASK_2"))
                .andRespond(withSuccess("{\"task\":\"TASK_1\",\"promptVersion\":null}", MediaType.APPLICATION_JSON));
        assertThrows(LearningRequestException.class, () -> client.activatedVersion("TASK_2"));
    }
}
