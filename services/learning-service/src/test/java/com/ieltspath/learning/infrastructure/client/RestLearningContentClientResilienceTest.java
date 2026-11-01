package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.commonresilience.ResilientCall;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import com.ieltspath.learning.application.exception.LearningRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import org.springframework.http.HttpStatus;

class RestLearningContentClientResilienceTest {
    private static final String BASE_URL = "http://content.test";
    private static final String LESSON_ID = "20000000-0000-4000-8000-000000000101";

    private MockRestServiceServer server;
    private RestLearningContentClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        var breakerConfig = CircuitBreakerConfig.custom().slidingWindowSize(20).minimumNumberOfCalls(10)
                .failureRateThreshold(50).recordException(ResilientCall::isTransientFailure).build();
        var retryConfig = RetryConfig.custom().maxAttempts(2).waitDuration(Duration.ofMillis(1))
                .retryOnException(ResilientCall::isTransientFailure).build();
        var resilience = new ResilientCall(CircuitBreakerRegistry.of(breakerConfig),
                RetryRegistry.of(retryConfig), BulkheadRegistry.ofDefaults());
        client = new RestLearningContentClient(builder.build(), resilience);
        var jwt = Jwt.withTokenValue("test-internal-token").header("alg", "HS256")
                .subject(UUID.randomUUID().toString()).issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        server.verify();
    }

    @Test
    void notFoundStaysNotFoundAndKeepsBreakerClosed() {
        server.expect(requestTo(BASE_URL + "/internal/learning-content/lessons/" + LESSON_ID))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo(BASE_URL + "/internal/learning-content/lessons/" + LESSON_ID))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        "{\"lessonId\":\"" + LESSON_ID + "\",\"topicId\":\"10000000-0000-4000-8000-000000000001\",\"code\":\"L1\",\"title\":\"Lesson\",\"sortOrder\":1,\"knowledgePointIds\":[],\"blocks\":[],\"skills\":[\"READING\"]}", MediaType.APPLICATION_JSON));

        var missing = assertThrows(LearningRequestException.class, () -> client.getLesson(UUID.fromString(LESSON_ID)));
        assertEquals(404, missing.getStatus());
        assertEquals("NOT_FOUND", missing.getCode());
        assertEquals(LESSON_ID, client.getLesson(UUID.fromString(LESSON_ID)).lessonId().toString());
    }

    @Test
    void repeatedServerErrorsOpenBreakerThenNoHttpRequestIsMade() {
        server.expect(times(10), requestTo(BASE_URL + "/internal/learning-content/lessons/" + LESSON_ID))
                .andRespond(withServerError());

        for (int attempt = 0; attempt < 5; attempt++) {
            var error = assertThrows(LearningRequestException.class,
                    () -> client.getLesson(UUID.fromString(LESSON_ID)));
            assertEquals(502, error.getStatus());
        }

        var unavailable = assertThrows(LearningRequestException.class,
                () -> client.getLesson(UUID.fromString(LESSON_ID)));
        assertEquals(503, unavailable.getStatus());
        assertEquals("CONTENT_UNAVAILABLE", unavailable.getCode());
    }
}
