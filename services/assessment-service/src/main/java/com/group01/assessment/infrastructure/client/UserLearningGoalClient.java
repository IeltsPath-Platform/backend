package com.group01.assessment.infrastructure.client;

import com.group01.assessment.application.port.LearningGoalProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserLearningGoalClient implements LearningGoalProvider {
    private final RestClient restClient;

    public UserLearningGoalClient(RestClient.Builder builder,
                                  @Value("${assessment.user.base-url:http://localhost:8085}") String userBaseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = builder.requestFactory(requestFactory).baseUrl(userBaseUrl).build();
    }

    @Override
    public Optional<UUID> findActiveGoalId(UUID userId) {
        ActiveGoalResponse goal;
        try {
            goal = restClient.get()
                    .uri("/api/users/me/learning-goals/active")
                    .header(HttpHeaders.AUTHORIZATION, GatewayRequestContext.bearerToken())
                    .header("X-Correlation-Id", GatewayRequestContext.correlationId())
                    .retrieve()
                    .body(ActiveGoalResponse.class);
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) return Optional.empty();
            throw exception;
        }
        if (goal == null || goal.id() == null || goal.userId() == null) {
            throw new IllegalStateException("User Service returned an invalid active learning goal");
        }
        if (!goal.userId().equals(userId)) {
            throw new IllegalStateException("Active learning goal does not belong to the authenticated learner");
        }
        return "ACTIVE".equalsIgnoreCase(goal.status()) ? Optional.of(goal.id()) : Optional.empty();
    }

    private record ActiveGoalResponse(UUID id, UUID userId, String status) {}
}
