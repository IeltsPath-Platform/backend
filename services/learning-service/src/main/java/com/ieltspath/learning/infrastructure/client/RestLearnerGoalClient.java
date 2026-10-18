package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.commonsecurity.header.SecurityHeaders;
import com.ieltspath.learning.application.port.LearnerGoalClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** Reads the learning goal from the User Service with the caller's bearer token; any failure means no goal. */
@Component
public class RestLearnerGoalClient implements LearnerGoalClient {
    private static final Logger log = LoggerFactory.getLogger(RestLearnerGoalClient.class);
    private final RestClient restClient;

    @Autowired
    public RestLearnerGoalClient(RestClient.Builder builder,
                                 @Value("${learning.user.base-url:http://localhost:8081}") String userBaseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        restClient = builder.requestFactory(requestFactory).baseUrl(userBaseUrl).build();
    }

    RestLearnerGoalClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<Goal> goal(UUID userId) {
        try {
            Optional<String> bearer = GatewayRequestHeaders.bearerToken();
            if (bearer.isEmpty()) return Optional.empty();
            Goal goal = restClient.get().uri("/internal/users/{userId}/learning-goal", userId)
                    .headers((HttpHeaders headers) -> {
                        headers.setBearerAuth(bearer.get());
                        headers.set(SecurityHeaders.CORRELATION_ID, GatewayRequestHeaders.correlationId());
                    }).retrieve().body(Goal.class);
            return Optional.ofNullable(goal);
        } catch (RuntimeException failure) {
            log.warn("Learning goal unavailable: userId={}", userId, failure);
            return Optional.empty();
        }
    }
}
