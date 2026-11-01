package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.commonsecurity.header.SecurityHeaders;
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
        client = new RestLearningContentClient(builder.build());
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

        for (int attempt = 0; attempt < 10; attempt++) {
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
